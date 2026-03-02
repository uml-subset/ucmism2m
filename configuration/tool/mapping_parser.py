"""
mapping_parser.py — shared parsing logic for ddi-cdi2ddsc mapping JSON.

Provides:
  - load_config(path)          : load and return the parsed JSON
  - get_known_classes(config)  : set of all class names appearing as source or target
  - parse_association_name(name, known_classes)
                               : split "Subject_predicate_Object" into
                                 (subject, predicate, object) using known_classes
                                 to anchor both ends
"""

import json
import sys


# ---------------------------------------------------------------------------
# JSON loading
# ---------------------------------------------------------------------------

def load_config(path: str) -> dict:
    """Load the mapping JSON from *path* and return the parsed dict."""
    try:
        with open(path, encoding="utf-8") as fh:
            return json.load(fh)
    except FileNotFoundError:
        sys.exit(f"Error: file not found: {path}")
    except json.JSONDecodeError as exc:
        sys.exit(f"Error: invalid JSON in {path}: {exc}")


# ---------------------------------------------------------------------------
# Known-class extraction
# ---------------------------------------------------------------------------

def get_known_classes(config: dict) -> set:
    """
    Return the set of all UML class names that appear as source or target
    in the class-mapping section.  These are used to anchor association-name
    parsing.
    """
    classes: set = set()
    for cm in config.get("mapping", {}).get("class", []):
        # target is always a single string
        if "targetClass" in cm:
            classes.add(cm["targetClass"])
        # sourceClass may be a string or a list (merge mappings)
        src = cm.get("sourceClass")
        if isinstance(src, str):
            classes.add(src)
        elif isinstance(src, list):
            classes.update(src)
    return classes


# ---------------------------------------------------------------------------
# Association-name parsing
# ---------------------------------------------------------------------------

def parse_association_name(name: str, known_classes: set) -> tuple:
    """
    Split an association name of the form "SubjectClass_predicate_ObjectClass"
    into a 3-tuple (subject_class, predicate, object_class).

    Strategy
    --------
    Association names encode a triple where subject and object are UML class
    names (possibly containing underscores, e.g. "WideDataSet") and the
    predicate is the middle portion.  Because underscores appear inside class
    names we cannot split naively; instead we use the *known_classes* set to
    anchor both ends:

    1. Try every prefix of the underscore-delimited tokens as a candidate
       subject.  Prefer the longest prefix whose value is a known class.
    2. From the remaining suffix, try every suffix as a candidate object.
       Prefer the longest suffix whose value is a known class.
    3. Whatever remains in the middle is the predicate.

    Falls back to a naive first/last token split if no known-class anchor
    is found (with a warning to stderr).
    """
    parts = name.split("_")
    n = len(parts)

    # --- anchor subject (longest known prefix) ---
    subject = None
    subject_len = 0
    for i in range(1, n):                      # at least one token for subject
        candidate = "_".join(parts[:i])
        if candidate in known_classes:
            subject = candidate
            subject_len = i

    # --- anchor object (longest known suffix after subject) ---
    obj = None
    obj_len = 0
    start = subject_len if subject_len > 0 else 1
    for j in range(1, n - start + 1):          # at least one token for object
        candidate = "_".join(parts[n - j:])
        if candidate in known_classes:
            obj = candidate
            obj_len = j

    # --- predicate is whatever is left in the middle ---
    if subject and obj and (subject_len + obj_len) < n:
        predicate_parts = parts[subject_len: n - obj_len]
        predicate = "_".join(predicate_parts)
        return subject, predicate, obj

    # --- fallback: naive first / last token ---
    print(
        f"Warning: could not anchor both ends of association '{name}' "
        f"using known classes; falling back to naive split.",
        file=sys.stderr,
    )
    return parts[0], "_".join(parts[1:-1]), parts[-1]


# ---------------------------------------------------------------------------
# Convenience: fix known typo in SSSOM predicate_id values
# ---------------------------------------------------------------------------

_TYPO_MAP = {
    "skos:narrorMatch": "skos:narrowMatch",
}


def fix_predicate_id(pid: str) -> str:
    """Return the corrected predicate_id, fixing any known typos."""
    return _TYPO_MAP.get(pid, pid)
