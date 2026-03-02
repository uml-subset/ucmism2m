#!/usr/bin/env python3
"""
mapping2rdf.py — convert a ddi-cdi2ddsc mapping JSON to RDF Turtle.

Usage:
    python mapping2rdf.py <input.json> [output.ttl]

If output.ttl is omitted the Turtle is written to stdout.

Vocabulary decisions
--------------------
Namespaces:
    cdi:      http://ddialliance.org/Specification/DDI-CDI/1.0/RDF/
    ddsc:     http://w3id.org/cdif/ddsc/1.0/
    transform: http://w3id.org/ucmis/vocab/transform#
    uml:      http://www.omg.org/spec/UML/2.5.1/
    prov:     http://www.w3.org/ns/prov#
    skos:     http://www.w3.org/2004/02/skos/core#
    sssom:    https://w3id.org/sssom/
    xsd:      http://www.w3.org/2001/XMLSchema#
    rdfs:     http://www.w3.org/2000/01/rdf-schema#

Resources:
    Transformation activity  → blank node, prov:Activity
    Source / target models   → fragment URIs, uml:Model
    Packages                 → fragment URIs under ddsc:, uml:Package
    Class mappings           → fragment URIs, transform:ClassMapping
    Attributes               → blank nodes, uml:Attribute
    SSSOM mappings           → fragment URIs, sssom:Mapping
    Association mappings     → fragment URIs, transform:AssociationMapping
"""

import sys
import re
from mapping_parser import load_config, get_known_classes, parse_association_name, fix_predicate_id

# ---------------------------------------------------------------------------
# Namespace declarations
# ---------------------------------------------------------------------------

PREFIXES = {
    "rdf":       "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs":      "http://www.w3.org/2000/01/rdf-schema#",
    "xsd":       "http://www.w3.org/2001/XMLSchema#",
    "prov":      "http://www.w3.org/ns/prov#",
    "skos":      "http://www.w3.org/2004/02/skos/core#",
    "sssom":     "https://w3id.org/sssom/",
    "uml":       "http://www.omg.org/spec/UML/2.5.1/",
    "transform": "http://w3id.org/ucmis/vocab/transform#",
    "cdi":       "http://ddialliance.org/Specification/DDI-CDI/1.0/RDF/",
    "ddsc":      "http://w3id.org/cdif/ddsc/1.0/",
    "dcterms":   "http://purl.org/dc/terms/",
}

# ---------------------------------------------------------------------------
# Turtle serialisation helpers
# ---------------------------------------------------------------------------

def _uri(prefix: str, local: str) -> str:
    """Return a prefixed name, e.g. 'ddsc:DataStore'."""
    return f"{prefix}:{local}"


def _literal(value: str) -> str:
    """Return a double-quoted Turtle string literal, escaping internals."""
    escaped = value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
    return f'"{escaped}"'


def _typed_literal(value, datatype: str) -> str:
    """Return a typed Turtle literal, e.g. '1.0'^^xsd:decimal ."""
    return f'"{value}"^^{datatype}'


def _safe_local(name: str) -> str:
    """
    Turn an arbitrary string into a safe Turtle local name fragment
    (replace spaces and special chars with underscores).
    """
    return re.sub(r"[^A-Za-z0-9_\-.]", "_", name)


def _write_prefix_block(lines: list) -> None:
    for prefix, uri in PREFIXES.items():
        lines.append(f"@prefix {prefix}: <{uri}> .")
    lines.append("")


# ---------------------------------------------------------------------------
# Section writers
# ---------------------------------------------------------------------------

def _write_transformation(lines: list, config: dict) -> None:
    """Emit the prov:Activity blank node for the transformation."""
    t = config["transformation"]
    src = t["sourceModel"]
    tgt = t["targetModel"]

    src_model_name = src.get("mainPackage", src.get("name", "SourceModel"))
    tgt_model_name = tgt.get("mainPackage", tgt.get("acronym", "TargetModel"))

    src_uri = _uri("cdi", _safe_local(src_model_name))
    tgt_uri = _uri("ddsc", _safe_local(tgt_model_name))

    lines.append("# ── Transformation activity ──────────────────────────────────────────────────")
    lines.append("")

    # Source model
    lines.append(f"{src_uri}")
    lines.append(f"    a uml:Model ;")
    lines.append(f"    rdfs:label {_literal(src.get('name', src_model_name))} ;")
    lines.append(f"    dcterms:identifier {_literal(src_model_name)} ;")
    lines.append(f"    rdfs:isDefinedBy <{src['uri']}> .")
    lines.append("")

    # Target model
    tgt_label = tgt.get("modelTitle", tgt.get("acronym", tgt_model_name))
    lines.append(f"{tgt_uri}")
    lines.append(f"    a uml:Model ;")
    lines.append(f"    rdfs:label {_literal(tgt_label)} ;")
    lines.append(f"    dcterms:identifier {_literal(tgt_model_name)} ;")
    if tgt.get("definition"):
        lines.append(f"    rdfs:comment {_literal(tgt['definition'])} ;")
    lines.append(f"    rdfs:isDefinedBy <{tgt['uri']}> .")
    lines.append("")

    # Transformation activity (blank node)
    lines.append("[] a prov:Activity ;")
    lines.append(f"    rdfs:label {_literal(t.get('description', 'DDI-CDI to DDSC transformation'))} ;")
    lines.append(f"    dcterms:hasVersion {_literal(t.get('version', ''))} ;")
    lines.append(f"    prov:used {src_uri} ;")
    lines.append(f"    prov:generated {tgt_uri} .")
    lines.append("")


def _write_packages(lines: list, config: dict) -> None:
    """Emit uml:Package resources for each package entry."""
    packages = config.get("package", [])
    if not packages:
        return

    lines.append("# ── Packages ─────────────────────────────────────────────────────────────────")
    lines.append("")

    for pkg in packages:
        name = pkg["name"]
        parent = pkg.get("parent", "")
        definition = pkg.get("definition", "")
        pkg_uri = _uri("ddsc", _safe_local(name))
        lines.append(f"{pkg_uri}")
        lines.append(f"    a uml:Package ;")
        lines.append(f"    rdfs:label {_literal(name)} ;")
        if definition:
            lines.append(f"    rdfs:comment {_literal(definition)} ;")
        if parent:
            lines.append(f"    transform:parentPackage {_uri('ddsc', _safe_local(parent))} ;")
        # Remove trailing semicolon from last predicate, add full stop
        # Rewrite last line
        last = lines[-1]
        if last.endswith(" ;"):
            lines[-1] = last[:-2] + " ."
        else:
            lines.append("    .")
        lines.append("")


def _write_class_mappings(lines: list, config: dict) -> None:
    """
    Emit transform:ClassMapping resources, uml:Attribute blank nodes,
    and sssom:Mapping resources.
    """
    class_mappings = config.get("mapping", {}).get("class", [])
    if not class_mappings:
        return

    lines.append("# ── Class mappings ───────────────────────────────────────────────────────────")
    lines.append("")

    for cm in class_mappings:
        mapping_type = cm["mappingType"]
        target_class = cm["targetClass"]
        target_pkg = cm.get("targetPackage", "")
        mapping_uri = _uri("ddsc", _safe_local(f"ClassMapping_{target_class}"))

        # Collect attribute blank nodes first
        attr_bnode_lines = []
        attr_refs = []
        for attr in cm.get("attribute", []):
            attr_name = attr["name"]
            mult = attr.get("multiplicity", {})
            lower = mult.get("lower", "0")
            upper = mult.get("upper", "*")
            from_src = attr.get("fromSourceClass", "")

            bnode_lines = ["    ["]
            bnode_lines.append(f"        a uml:Attribute ;")
            bnode_lines.append(f"        rdfs:label {_literal(attr_name)} ;")
            bnode_lines.append(f"        transform:multiplicityLower {_literal(str(lower))} ;")
            bnode_lines.append(f"        transform:multiplicityUpper {_literal(str(upper))} ;")
            if from_src:
                bnode_lines.append(
                    f"        transform:fromSourceClass {_uri('cdi', _safe_local(from_src))} ;"
                )
            # Fix trailing semicolons inside blank node
            last_inner = bnode_lines[-1]
            if last_inner.endswith(" ;"):
                bnode_lines[-1] = last_inner[:-2]
            bnode_lines.append("    ]")
            attr_refs.append("\n".join(bnode_lines))

        # Collect SSSOM mapping URIs and their triples
        sssom_entries = cm.get("sssom", [])
        sssom_uris = []
        sssom_blocks = []
        for idx, sssom in enumerate(sssom_entries, start=1):
            sssom_uri = _uri(
                "ddsc",
                _safe_local(f"SSSOMMapping_{target_class}_{idx}")
            )
            sssom_uris.append(sssom_uri)
            predicate_id = fix_predicate_id(sssom.get("predicate_id", ""))
            # Resolve skos: predicate to full prefixed name
            # predicate_id is already in "skos:xxx" form
            confidence = sssom.get("confidence", "")
            comment = sssom.get("comment", "")
            subject_label = sssom.get("subject_label", "")
            object_label = sssom.get("object_label", "")
            predicate_label = sssom.get("predicate_label", "")

            block = [f"{sssom_uri}"]
            block.append(f"    a sssom:Mapping ;")
            if subject_label:
                block.append(f"    sssom:subject_label {_literal(subject_label)} ;")
            if object_label:
                block.append(f"    sssom:object_label {_literal(object_label)} ;")
            if predicate_id:
                block.append(f"    sssom:predicate_id {_literal(predicate_id)} ;")
            if predicate_label:
                block.append(f"    sssom:predicate_label {_literal(predicate_label)} ;")
            if confidence != "":
                block.append(
                    f"    sssom:confidence {_typed_literal(confidence, 'xsd:decimal')} ;"
                )
            if comment:
                block.append(f"    rdfs:comment {_literal(comment)} ;")
            # Fix last semicolon
            last_b = block[-1]
            if last_b.endswith(" ;"):
                block[-1] = last_b[:-2] + " ."
            else:
                block.append("    .")
            sssom_blocks.append("\n".join(block))

        # Emit the ClassMapping resource
        lines.append(f"{mapping_uri}")
        lines.append(f"    a transform:ClassMapping ;")
        lines.append(f"    transform:mappingType {_literal(mapping_type)} ;")

        # Source class(es)
        src = cm.get("sourceClass", cm.get("sourceClasses", []))
        if isinstance(src, str):
            lines.append(f"    transform:sourceClass {_uri('cdi', _safe_local(src))} ;")
        elif isinstance(src, list):
            src_uris = ", ".join(_uri("cdi", _safe_local(s)) for s in src)
            lines.append(f"    transform:sourceClass {src_uris} ;")

        lines.append(f"    transform:targetClass {_uri('ddsc', _safe_local(target_class))} ;")

        if target_pkg:
            lines.append(
                f"    transform:targetPackage {_uri('ddsc', _safe_local(target_pkg))} ;"
            )

        definition_from = cm.get("definitionFrom", "")
        if definition_from:
            lines.append(
                f"    transform:definitionFrom {_uri('cdi', _safe_local(definition_from))} ;"
            )

        # Attributes
        if attr_refs:
            for i, attr_block in enumerate(attr_refs):
                sep = ";" if (i < len(attr_refs) - 1 or sssom_uris) else ""
                lines.append(f"    transform:hasAttribute")
                lines.append(f"{attr_block}{' ' + sep if sep else ''}")

        # SSSOM references
        if sssom_uris:
            sssom_ref = ", ".join(sssom_uris)
            lines.append(f"    transform:hasMapping {sssom_ref} .")
        else:
            # Fix last predicate line to end with .
            for i in range(len(lines) - 1, -1, -1):
                if lines[i].strip().endswith(" ;"):
                    lines[i] = lines[i][:-2] + " ."
                    break

        lines.append("")

        # Emit SSSOM blocks after the class mapping
        for block in sssom_blocks:
            lines.append(block)
            lines.append("")

    lines.append("")


def _write_association_mappings(lines: list, config: dict, known_classes: set) -> None:
    """Emit transform:AssociationMapping resources."""
    assoc_mappings = config.get("mapping", {}).get("association", [])
    if not assoc_mappings:
        return

    lines.append("# ── Association mappings ─────────────────────────────────────────────────────")
    lines.append("")

    for am in assoc_mappings:
        src_name = am.get("sourceAssociationName", "")
        tgt_name = am.get("targetAssociationName", src_name)
        mult = am.get("objectClassMultiplicity", {})
        lower = mult.get("lower", "0")
        upper = mult.get("upper", "*")

        subject, predicate, obj = parse_association_name(src_name, known_classes)

        mapping_uri = _uri("ddsc", _safe_local(f"AssocMapping_{tgt_name}"))

        lines.append(f"{mapping_uri}")
        lines.append(f"    a transform:AssociationMapping ;")
        lines.append(f"    transform:sourceAssociationName {_literal(src_name)} ;")
        lines.append(f"    transform:targetAssociationName {_literal(tgt_name)} ;")
        lines.append(f"    transform:subjectClass {_uri('ddsc', _safe_local(subject))} ;")
        lines.append(f"    rdfs:label {_literal(predicate)} ;")
        lines.append(f"    transform:objectClass {_uri('ddsc', _safe_local(obj))} ;")
        lines.append(f"    transform:objectClassMultiplicityLower {_literal(str(lower))} ;")
        lines.append(f"    transform:objectClassMultiplicityUpper {_literal(str(upper))} .")
        lines.append("")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def generate_turtle(config: dict) -> str:
    known_classes = get_known_classes(config)
    lines = []

    # File-level comment
    t = config.get("transformation", {})
    lines.append(
        f"# RDF Turtle representation of the DDI-CDI to DDSC mapping configuration"
    )
    lines.append(f"# Description : {t.get('description', '')}")
    lines.append(f"# Version     : {t.get('version', '')}")
    lines.append("")

    _write_prefix_block(lines)
    _write_transformation(lines, config)
    _write_packages(lines, config)
    _write_class_mappings(lines, config)
    _write_association_mappings(lines, config, known_classes)

    return "\n".join(lines)


def main():
    if len(sys.argv) < 2:
        print("Usage: python mapping2rdf.py <input.json> [output.ttl]", file=sys.stderr)
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) >= 3 else None

    config = load_config(input_path)
    turtle = generate_turtle(config)

    if output_path:
        with open(output_path, "w", encoding="utf-8") as fh:
            fh.write(turtle)
        print(f"Written: {output_path}", file=sys.stderr)
    else:
        print(turtle)


if __name__ == "__main__":
    main()
