"""Gera as variantes da Rupty com a lamina encurtada (fragmentos fora da lamina).

Para cada estagio (dormente, desperta, ressonante, final) cria rupty_<estagio>_f1..f5.json,
escondendo de 1 a 5 pecas da lamina a partir da ponta (lamina_seg1..4 + lamina_ponta), junto com
estilhacos e fios que ficam acima do corte. Tambem reescreve assets/nexus/items/rupty.json.
Rode de novo sempre que editar os modelos no Blockbench:  python scripts/gerar_rupty_lamina.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nexus")
MODELS = os.path.join(ROOT, "models", "item")
STAGES = ["dormente", "desperta", "ressonante", "final"]
PIECES = ["lamina_seg1", "lamina_seg2", "lamina_seg3", "lamina_seg4", "lamina_ponta"]
BLADE_PREFIXES = ("lamina_", "estilhaco_", "fio_")


def variant(model, hidden):
    by_name = {e.get("name"): e for e in model["elements"]}
    kept = PIECES[: len(PIECES) - hidden]
    # Corte: topo da ultima peca mantida; sem pecas, tudo da lamina some.
    cut = max(by_name[p]["to"][1] for p in kept) if kept else float("-inf")
    elements = []
    for e in model["elements"]:
        name = e.get("name", "")
        if name.startswith(BLADE_PREFIXES):
            if name in PIECES:
                if name not in kept:
                    continue
            elif not kept or e["from"][1] >= cut - 0.01:
                continue
        elements.append(e)
    result = {k: v for k, v in model.items() if k not in ("elements", "groups", "outliner")}
    result["elements"] = elements
    return result


def main():
    entries = []
    for index, stage in enumerate(STAGES):
        base = f"rupty_{stage}"
        with open(os.path.join(MODELS, base + ".json"), encoding="utf-8") as f:
            model = json.load(f)
        for hidden in range(1, len(PIECES) + 1):
            with open(os.path.join(MODELS, f"{base}_f{hidden}.json"), "w", encoding="utf-8") as f:
                json.dump(variant(model, hidden), f, indent="\t")
        blade = {
            "type": "minecraft:range_dispatch",
            "property": "minecraft:custom_model_data",
            "index": 1,
            "fallback": {"type": "minecraft:model", "model": f"nexus:item/{base}"},
            "entries": [{"threshold": h, "model": {"type": "minecraft:model", "model": f"nexus:item/{base}_f{h}"}}
                        for h in range(1, len(PIECES) + 1)],
        }
        entries.append((index, blade))
    item = {"model": {
        "type": "minecraft:range_dispatch",
        "property": "minecraft:custom_model_data",
        "index": 0,
        "fallback": entries[0][1],
        "entries": [{"threshold": i, "model": m} for i, m in entries[1:]],
    }}
    with open(os.path.join(ROOT, "items", "rupty.json"), "w", encoding="utf-8") as f:
        json.dump(item, f, indent=2)
    print("Variantes geradas.")


if __name__ == "__main__":
    main()
