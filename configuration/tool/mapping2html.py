#!/usr/bin/env python3
"""
mapping2html.py — render a ddi-cdi2ddsc mapping JSON as a human-readable HTML page.

Usage:
    python mapping2html.py <input.json> [output.html]

If output.html is omitted the HTML is written to stdout.

Layout
------
1. Header block  — transformation metadata (description, version, source/target models)
2. Package table — packages defined in the target model
3. Class mappings table
   - Mapping type shown as a colour-coded badge (map / merge / new)
   - Source class(es), target class, target package, attributes with multiplicity
   - SSSOM metadata shown in two ways in the same page:
       (a) Expandable sub-row inline within the class table (Bootstrap collapse)
       (b) Separate SSSOM reference table at the bottom, cross-referenced by
           target class name
4. Association mappings table
   - Association names with resolved subject / predicate / object
   - Object-class multiplicity

Dependencies: Bootstrap 5 (CDN), no other external JS/CSS.
"""

import sys
import html as html_module
from mapping_parser import load_config, get_known_classes, parse_association_name, fix_predicate_id

# ---------------------------------------------------------------------------
# Badge colours per mapping type
# ---------------------------------------------------------------------------

_BADGE = {
    "map":   "bg-primary",
    "merge": "bg-warning text-dark",
    "new":   "bg-success",
}


def _badge(mapping_type: str) -> str:
    css = _BADGE.get(mapping_type, "bg-secondary")
    return f'<span class="badge {css}">{html_module.escape(mapping_type)}</span>'


def _esc(value) -> str:
    """HTML-escape a value (convert to string first)."""
    return html_module.escape(str(value) if value is not None else "")


def _mult(mult: dict) -> str:
    lower = mult.get("lower", "0")
    upper = mult.get("upper", "*")
    return f"{lower}..{upper}"


# ---------------------------------------------------------------------------
# Section builders
# ---------------------------------------------------------------------------

def _header_html(config: dict) -> str:
    t = config["transformation"]
    src = t["sourceModel"]
    tgt = t["targetModel"]
    src_label = _esc(src.get("name", src.get("mainPackage", "")))
    tgt_label = _esc(tgt.get("modelTitle", tgt.get("acronym", tgt.get("mainPackage", ""))))
    src_uri = _esc(src.get("uri", ""))
    tgt_uri = _esc(tgt.get("uri", ""))
    description = _esc(t.get("description", ""))
    version = _esc(t.get("version", ""))

    return f"""
<div class="card mb-4 shadow-sm">
  <div class="card-header bg-dark text-white">
    <h4 class="mb-0">Mapping Configuration</h4>
  </div>
  <div class="card-body">
    <dl class="row mb-0">
      <dt class="col-sm-3">Description</dt>
      <dd class="col-sm-9">{description}</dd>
      <dt class="col-sm-3">Version</dt>
      <dd class="col-sm-9">{version}</dd>
      <dt class="col-sm-3">Source model</dt>
      <dd class="col-sm-9">{src_label} &mdash; <a href="{src_uri}" target="_blank">{src_uri}</a></dd>
      <dt class="col-sm-3">Target model</dt>
      <dd class="col-sm-9">{tgt_label} &mdash; <a href="{tgt_uri}" target="_blank">{tgt_uri}</a></dd>
    </dl>
  </div>
</div>
"""


def _packages_html(config: dict) -> str:
    packages = config.get("package", [])
    if not packages:
        return ""

    rows = ""
    for pkg in packages:
        rows += f"""
      <tr>
        <td><code>{_esc(pkg.get('name',''))}</code></td>
        <td><code>{_esc(pkg.get('parent',''))}</code></td>
        <td>{_esc(pkg.get('definition',''))}</td>
      </tr>"""

    return f"""
<h5 class="mt-4">Packages</h5>
<div class="table-responsive mb-4">
  <table class="table table-sm table-bordered table-hover align-middle">
    <thead class="table-light">
      <tr>
        <th>Package</th>
        <th>Parent</th>
        <th>Definition</th>
      </tr>
    </thead>
    <tbody>{rows}
    </tbody>
  </table>
</div>
"""


def _attr_list(attributes: list) -> str:
    if not attributes:
        return "<em class='text-muted'>—</em>"
    items = []
    for attr in attributes:
        name = _esc(attr.get("name", ""))
        mult = attr.get("multiplicity", {})
        m = _esc(_mult(mult))
        from_src = attr.get("fromSourceClass", "")
        extra = f" <small class='text-muted'>(from {_esc(from_src)})</small>" if from_src else ""
        items.append(f"<code>{name}</code> [{m}]{extra}")
    return "<br>".join(items)


def _sssom_inline(sssom_entries: list, collapse_id: str) -> str:
    """Render SSSOM entries as a Bootstrap-collapsible sub-row content."""
    if not sssom_entries:
        return ""

    rows = ""
    for s in sssom_entries:
        predicate_id = fix_predicate_id(s.get("predicate_id", ""))
        rows += f"""
          <tr>
            <td>{_esc(s.get('subject_label',''))}</td>
            <td>{_esc(s.get('object_label',''))}</td>
            <td><code>{_esc(predicate_id)}</code></td>
            <td>{_esc(s.get('predicate_label',''))}</td>
            <td>{_esc(s.get('confidence',''))}</td>
            <td>{_esc(s.get('comment',''))}</td>
          </tr>"""

    return f"""
<div class="collapse" id="{collapse_id}">
  <div class="p-2 bg-light border-top">
    <small><strong>SSSOM Mappings</strong></small>
    <table class="table table-sm table-bordered mt-1 mb-0">
      <thead class="table-secondary">
        <tr>
          <th>Subject label</th>
          <th>Object label</th>
          <th>Predicate ID</th>
          <th>Predicate label</th>
          <th>Confidence</th>
          <th>Comment</th>
        </tr>
      </thead>
      <tbody>{rows}
      </tbody>
    </table>
  </div>
</div>"""


def _class_mappings_html(config: dict) -> str:
    class_mappings = config.get("mapping", {}).get("class", [])
    if not class_mappings:
        return ""

    rows = ""
    for i, cm in enumerate(class_mappings):
        mapping_type = cm.get("mappingType", "")
        target_class = cm.get("targetClass", "")
        target_pkg = cm.get("targetPackage", "")
        definition_from = cm.get("definitionFrom", "")
        attributes = cm.get("attribute", [])
        sssom_entries = cm.get("sssom", [])

        # Source class(es)
        src = cm.get("sourceClass", cm.get("sourceClasses", []))
        if isinstance(src, str):
            src_display = f"<code>{_esc(src)}</code>"
        elif isinstance(src, list):
            src_display = "<br>".join(f"<code>{_esc(s)}</code>" for s in src)
        else:
            src_display = ""

        # Extra info cell
        extra_parts = []
        if definition_from:
            extra_parts.append(f"Definition from: <code>{_esc(definition_from)}</code>")
        extra_html = "<br>".join(extra_parts) if extra_parts else ""

        # SSSOM toggle button
        collapse_id = f"sssom_inline_{i}"
        sssom_btn = ""
        sssom_collapse = ""
        if sssom_entries:
            sssom_btn = (
                f' <button class="btn btn-outline-info btn-sm py-0 px-1 ms-1" '
                f'type="button" data-bs-toggle="collapse" '
                f'data-bs-target="#{collapse_id}" '
                f'aria-expanded="false" aria-controls="{collapse_id}">'
                f'SSSOM</button>'
            )
            sssom_collapse = _sssom_inline(sssom_entries, collapse_id)

        rows += f"""
      <tr>
        <td>{_badge(mapping_type)}</td>
        <td>{src_display}</td>
        <td><code>{_esc(target_class)}</code>{sssom_btn}
          {sssom_collapse}
        </td>
        <td><code>{_esc(target_pkg)}</code></td>
        <td>{_attr_list(attributes)}</td>
        <td>{extra_html}</td>
      </tr>"""

    return f"""
<h5 class="mt-4">Class Mappings
  <span class="badge bg-secondary ms-2">{len(class_mappings)}</span>
</h5>
<p class="text-muted small">Click <span class="badge bg-outline border border-info text-info">SSSOM</span> to expand inline SSSOM details. A separate SSSOM reference table is below.</p>
<div class="table-responsive mb-4">
  <table class="table table-sm table-bordered table-hover align-middle">
    <thead class="table-light">
      <tr>
        <th>Type</th>
        <th>Source class(es)</th>
        <th>Target class</th>
        <th>Target package</th>
        <th>Attributes</th>
        <th>Notes</th>
      </tr>
    </thead>
    <tbody>{rows}
    </tbody>
  </table>
</div>
"""


def _sssom_reference_table_html(config: dict) -> str:
    """Render all SSSOM entries as a separate flat reference table."""
    class_mappings = config.get("mapping", {}).get("class", [])
    all_rows = ""
    total = 0

    for cm in class_mappings:
        target_class = cm.get("targetClass", "")
        sssom_entries = cm.get("sssom", [])
        for s in sssom_entries:
            total += 1
            predicate_id = fix_predicate_id(s.get("predicate_id", ""))
            all_rows += f"""
      <tr>
        <td><code>{_esc(target_class)}</code></td>
        <td>{_esc(s.get('subject_label',''))}</td>
        <td>{_esc(s.get('object_label',''))}</td>
        <td><code>{_esc(predicate_id)}</code></td>
        <td>{_esc(s.get('predicate_label',''))}</td>
        <td>{_esc(s.get('confidence',''))}</td>
        <td>{_esc(s.get('comment',''))}</td>
      </tr>"""

    if not total:
        return ""

    return f"""
<h5 class="mt-5">SSSOM Mapping Reference
  <span class="badge bg-secondary ms-2">{total}</span>
</h5>
<p class="text-muted small">All SSSOM mapping entries, cross-referenced by target class.</p>
<div class="table-responsive mb-4">
  <table class="table table-sm table-bordered table-hover align-middle">
    <thead class="table-light">
      <tr>
        <th>Target class</th>
        <th>Subject label</th>
        <th>Object label</th>
        <th>Predicate ID</th>
        <th>Predicate label</th>
        <th>Confidence</th>
        <th>Comment</th>
      </tr>
    </thead>
    <tbody>{all_rows}
    </tbody>
  </table>
</div>
"""


def _association_mappings_html(config: dict, known_classes: set) -> str:
    assoc_mappings = config.get("mapping", {}).get("association", [])
    if not assoc_mappings:
        return ""

    rows = ""
    for am in assoc_mappings:
        src_name = am.get("sourceAssociationName", "")
        tgt_name = am.get("targetAssociationName", src_name)
        mult = am.get("objectClassMultiplicity", {})
        mult_str = _mult(mult)

        subject, predicate, obj = parse_association_name(src_name, known_classes)

        # Highlight when source != target name
        changed = "table-warning" if src_name != tgt_name else ""

        rows += f"""
      <tr class="{changed}">
        <td><code>{_esc(src_name)}</code></td>
        <td><code>{_esc(tgt_name)}</code></td>
        <td><code>{_esc(subject)}</code></td>
        <td><code>{_esc(predicate)}</code></td>
        <td><code>{_esc(obj)}</code></td>
        <td><code>{_esc(mult_str)}</code></td>
      </tr>"""

    return f"""
<h5 class="mt-4">Association Mappings
  <span class="badge bg-secondary ms-2">{len(assoc_mappings)}</span>
</h5>
<p class="text-muted small">Rows highlighted in yellow indicate associations where the target name differs from the source name.</p>
<div class="table-responsive mb-4">
  <table class="table table-sm table-bordered table-hover align-middle">
    <thead class="table-light">
      <tr>
        <th>Source association</th>
        <th>Target association</th>
        <th>Subject class</th>
        <th>Predicate</th>
        <th>Object class</th>
        <th>Object multiplicity</th>
      </tr>
    </thead>
    <tbody>{rows}
    </tbody>
  </table>
</div>
"""


# ---------------------------------------------------------------------------
# Full page assembly
# ---------------------------------------------------------------------------

def generate_html(config: dict) -> str:
    known_classes = get_known_classes(config)
    t = config["transformation"]
    page_title = _esc(t.get("description", "DDI-CDI to DDSC Mapping"))

    header      = _header_html(config)
    packages    = _packages_html(config)
    classes     = _class_mappings_html(config)
    assocs      = _association_mappings_html(config, known_classes)
    sssom_ref   = _sssom_reference_table_html(config)

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{page_title}</title>
  <link
    rel="stylesheet"
    href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
    integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
    crossorigin="anonymous"
  >
  <style>
    body {{ font-size: 0.9rem; }}
    th   {{ white-space: nowrap; }}
    code {{ font-size: 0.85em; }}
    .table td, .table th {{ vertical-align: middle; }}
    .collapse-sssom {{ font-size: 0.82rem; }}
  </style>
</head>
<body>
<div class="container-fluid py-4">
  <h2 class="mb-4">{page_title}</h2>

  {header}
  {packages}
  {classes}
  {assocs}
  {sssom_ref}

</div>
<script
  src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
  integrity="sha384-YvpcrYf0tY3lHB60NNkmXc4s9bIOgUxi8T/jzmkCA8DLZInuvHhS5tn3ZkAjvTzi"
  crossorigin="anonymous"
></script>
</body>
</html>
"""


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    if len(sys.argv) < 2:
        print("Usage: python mapping2html.py <input.json> [output.html]", file=sys.stderr)
        sys.exit(1)

    input_path  = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) >= 3 else None

    config = load_config(input_path)
    page   = generate_html(config)

    if output_path:
        with open(output_path, "w", encoding="utf-8") as fh:
            fh.write(page)
        print(f"Written: {output_path}", file=sys.stderr)
    else:
        print(page)


if __name__ == "__main__":
    main()
