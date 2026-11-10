"""Builds translated names that follow patterns (metal items, tiered blocks) for each language."""
import re

LANGS = ["zh_cn", "ru_ru", "es_es", "de_de", "pt_br"]

# ---- metals: zh, ru (nominative, genitive, gender), es, de, pt (name, feminine?)
METALS = {
    "Tin":      ("锡", ("олово", "олова", "n"), "estaño", "Zinn", ("estanho", False)),
    "Lead":     ("铅", ("свинец", "свинца", "m"), "plomo", "Blei", ("chumbo", False)),
    "Silver":   ("银", ("серебро", "серебра", "n"), "plata", "Silber", ("prata", True)),
    "Nickel":   ("镍", ("никель", "никеля", "m"), "níquel", "Nickel", ("níquel", False)),
    "Uranium":  ("铀", ("уран", "урана", "m"), "uranio", "Uran", ("urânio", False)),
    "Steel":    ("钢", ("сталь", "стали", "f"), "acero", "Stahl", ("aço", False)),
    "Bronze":   ("青铜", ("бронза", "бронзы", "f"), "bronce", "Bronze", ("bronze", False)),
    "Invar":    ("殷钢", ("инвар", "инвара", "m"), "invar", "Invar", ("invar", False)),
    "Electrum": ("琥珀金", ("электрум", "электрума", "m"), "electro", "Elektrum", ("electro", False)),
    "Copper":   ("铜", ("медь", "меди", "f"), "cobre", "Kupfer", ("cobre", False)),
    "Iron":     ("铁", ("железо", "железа", "n"), "hierro", "Eisen", ("ferro", False)),
    "Gold":     ("金", ("золото", "золота", "n"), "oro", "Gold", ("ouro", False)),
}
RU_RAW = {"m": "Рудный", "f": "Рудная", "n": "Рудное"}
RU_RAW_GEN = {"m": "рудного", "f": "рудной", "n": "рудного"}

# form: (zh template, ru template with {g}=genitive, es "X de {m}", de suffix (compound), pt "X de {m}")
FORMS = {
    "Ingot":      ("{m}锭", "Слиток {g}", "Lingote de {m}", "barren", "Barra de {m}"),
    "Dust":       ("{m}粉", "Пыль {g}", "Polvo de {m}", "staub", "Pó de {m}"),
    "Gear":       ("{m}齿轮", "Шестерня из {g}", "Engranaje de {m}", "zahnrad", "Engrenagem de {m}"),
    "Plate":      ("{m}板", "Пластина из {g}", "Placa de {m}", "platte", "Placa de {m}"),
    "Nugget":     ("{m}粒", "Самородок {g}", "Pepita de {m}", "nugget", "Pepita de {m}"),
    "Wire":       ("{m}线", "Проволока из {g}", "Cable de {m}", "draht", "Fio de {m}"),
    "Crystal":    ("{m}晶体", "Кристалл {g}", "Cristal de {m}", "kristall", "Cristal de {m}"),
    "Slurry":     ("{m}浆液", "Суспензия {g}", "Suspensión de {m}", "schlamm", "Pasta de {m}"),
    "Ore":        ("{m}矿石", "Руда {g}", "Mena de {m}", "erz", "Minério de {m}"),
    "Axe":        ("{m}斧", "Топор из {g}", "Hacha de {m}", "axt", "Machado de {m}"),
    "Boots":      ("{m}靴子", "Ботинки из {g}", "Botas de {m}", "stiefel", "Botas de {m}"),
    "Chestplate": ("{m}胸甲", "Нагрудник из {g}", "Peto de {m}", "brustplatte", "Peitoral de {m}"),
    "Helmet":     ("{m}头盔", "Шлем из {g}", "Casco de {m}", "helm", "Capacete de {m}"),
    "Hoe":        ("{m}锄", "Мотыга из {g}", "Azada de {m}", "hacke", "Enxada de {m}"),
    "Leggings":   ("{m}护腿", "Поножи из {g}", "Grebas de {m}", "beinschutz", "Calças de {m}"),
    "Pickaxe":    ("{m}镐", "Кирка из {g}", "Pico de {m}", "spitzhacke", "Picareta de {m}"),
    "Shovel":     ("{m}锹", "Лопата из {g}", "Pala de {m}", "schaufel", "Pá de {m}"),
    "Sword":      ("{m}剑", "Меч из {g}", "Espada de {m}", "schwert", "Espada de {m}"),
    "Coil":       ("{m}线圈", "Катушка из {g}", "Bobina de {m}", "spule", "Bobina de {m}"),
}
METAL_RE = re.compile(r"^(Deepslate |Raw |Block of Raw |Block of |Crushed )?(%s)( (%s))?( Ore)?$" % ("|".join(METALS), "|".join(FORMS)))


def metal_name(text, lang):
    m = METAL_RE.match(text)
    if not m:
        return None
    prefix, metal, form, ore = (m.group(1) or ""), m.group(2), m.group(4), bool(m.group(5))
    if form == "Ore":
        form, ore = None, True
    zh, (ru_n, ru_g, ru_gender), es, de, (pt, pt_f) = METALS[metal]
    if form and not ore:
        i = {"zh_cn": 0, "ru_ru": 1, "es_es": 2, "de_de": 3, "pt_br": 4}[lang]
        t = FORMS[form][i]
        if lang == "zh_cn": return t.format(m=zh)
        if lang == "ru_ru": return t.format(g=ru_g)
        if lang == "es_es": return t.format(m=es)
        if lang == "de_de": return de + t
        return t.format(m=pt)
    if prefix == "Raw ":
        return {"zh_cn": f"粗{zh}", "ru_ru": f"{RU_RAW[ru_gender]} {ru_n}", "es_es": f"{es.capitalize()} en bruto",
                "de_de": f"Roh{de.lower()}", "pt_br": f"{pt.capitalize()} {'bruta' if pt_f else 'bruto'}"}[lang]
    if prefix == "Block of Raw ":
        return {"zh_cn": f"粗{zh}块", "ru_ru": f"Блок {RU_RAW_GEN[ru_gender]} {ru_g}", "es_es": f"Bloque de {es} en bruto",
                "de_de": f"Roh{de.lower()}block", "pt_br": f"Bloco de {pt} {'bruta' if pt_f else 'bruto'}"}[lang]
    if prefix == "Block of ":
        return {"zh_cn": f"{zh}块", "ru_ru": f"Блок {ru_g}", "es_es": f"Bloque de {es}", "de_de": f"{de}block", "pt_br": f"Bloco de {pt}"}[lang]
    if prefix == "Deepslate " and ore:
        return {"zh_cn": f"深层{zh}矿石", "ru_ru": f"Глубинная руда {ru_g}", "es_es": f"Mena de {es} de pizarra profunda",
                "de_de": f"Tiefenschiefer-{de}erz", "pt_br": f"Minério de {pt} de ardósia profunda"}[lang]
    if prefix == "Crushed " and ore:
        return {"zh_cn": f"粉碎的{zh}矿石", "ru_ru": f"Дроблёная руда {ru_g}", "es_es": f"Mena de {es} triturada",
                "de_de": f"Zerkleinertes {de}erz", "pt_br": f"Minério de {pt} triturado"}[lang]
    if ore and not prefix:
        return metal_name(f"{metal} Ore", lang) if False else {"zh_cn": f"{zh}矿石", "ru_ru": f"Руда {ru_g}", "es_es": f"Mena de {es}",
                "de_de": f"{de}erz", "pt_br": f"Minério de {pt}"}[lang]
    return None


# ---- tiered names: adjective by gender (m, f, n, pl / es-pt: m, f, mpl, fpl)
TIERS = ["Basic", "Reinforced", "Advanced", "Elite", "Ultimate"]
ZH_TIER = {"Basic": "基础", "Reinforced": "强化", "Advanced": "高级", "Elite": "精英", "Ultimate": "终极"}
RU_TIER = {"Basic": "Базов", "Reinforced": "Усиленн", "Advanced": "Продвинут", "Elite": "Элитн", "Ultimate": "Совершенн"}
RU_END = {"m": "ый", "f": "ая", "n": "ое", "pl": "ые"}
ES_TIER = {"Basic": ("básico", "básica", "básicos", "básicas"), "Reinforced": ("reforzado", "reforzada", "reforzados", "reforzadas"),
           "Advanced": ("avanzado", "avanzada", "avanzados", "avanzadas"), "Elite": ("de élite",) * 4,
           "Ultimate": ("definitivo", "definitiva", "definitivos", "definitivas")}
PT_TIER = {"Basic": ("básico", "básica", "básicos", "básicas"), "Reinforced": ("reforçado", "reforçada", "reforçados", "reforçadas"),
           "Advanced": ("avançado", "avançada", "avançados", "avançadas"), "Elite": ("de elite",) * 4,
           "Ultimate": ("supremo", "suprema", "supremos", "supremas")}
DE_TIER = {"Basic": "Einfach", "Reinforced": "Verstärkt", "Advanced": "Fortgeschritten", "Ultimate": "Ultimativ"}
DE_END = {"m": "er", "f": "e", "n": "es", "pl": "e"}
ESG = {"m": 0, "f": 1, "mpl": 2, "fpl": 3}
# noun: zh, (ru, gender), (es, gender), (de, gender), (pt, gender)
NOUNS = {
    "Battery": ("电池", ("батарея", "f"), ("Batería", "f"), ("Batterie", "f"), ("Bateria", "f")),
    "Bulk Bin": ("大容量储物箱", ("бункер", "m"), ("Contenedor a granel", "m"), ("Massenbehälter", "m"), ("Contêiner a granel", "m")),
    "Crate": ("板条箱", ("ящик", "m"), ("Caja", "f"), ("Kiste", "f"), ("Caixote", "m")),
    "Energy Cable": ("能量线缆", ("энергокабель", "m"), ("Cable de energía", "m"), ("Energiekabel", "n"), ("Cabo de energia", "m")),
    "Energy Cell": ("能量电池", ("энергоячейка", "f"), ("Celda de energía", "f"), ("Energiezelle", "f"), ("Célula de energia", "f")),
    "Fluid Drive": ("流体驱动器", ("накопитель жидкостей", "m"), ("Unidad de fluidos", "f"), ("Fluidlaufwerk", "n"), ("Unidade de fluidos", "f")),
    "Fluid Pipe": ("流体管道", ("жидкостная труба", "f"), ("Tubería de fluidos", "f"), ("Fluidrohr", "n"), ("Cano de fluidos", "m")),
    "Fluid Tank": ("流体储罐", ("резервуар", "m"), ("Tanque de fluidos", "m"), ("Fluidtank", "m"), ("Tanque de fluidos", "m")),
    "Item Drive": ("物品驱动器", ("накопитель предметов", "m"), ("Unidad de objetos", "f"), ("Gegenstandslaufwerk", "n"), ("Unidade de itens", "f")),
    "Item Duct": ("物品导管", ("предметный трубопровод", "m"), ("Conducto de objetos", "m"), ("Gegenstandsleitung", "f"), ("Duto de itens", "m")),
    "Machine Casing": ("机器外壳", ("корпус машины", "m"), ("Carcasa de máquina", "f"), ("Maschinengehäuse", "n"), ("Carcaça de máquina", "f")),
    "Solar Panel": ("太阳能板", ("солнечная панель", "f"), ("Panel solar", "m"), ("Solarmodul", "n"), ("Painel solar", "m")),
    "Upgrade Kit": ("升级套件", ("набор улучшения", "m"), ("Kit de mejora", "m"), ("Upgrade-Set", "n"), ("Kit de melhoria", "m")),
    "Circuit": ("电路板", ("схема", "f"), ("Circuito", "m"), ("Schaltkreis", "m"), ("Circuito", "m")),
    "Pump": ("泵", ("насос", "m"), ("Bomba", "f"), ("Pumpe", "f"), ("Bomba", "f")),
    "Exo-Suit Boots": ("外骨骼靴子", ("ботинки экзокостюма", "pl"), ("Botas de exotraje", "fpl"), ("Exoanzug-Stiefel", "pl"), ("Botas de exotraje", "fpl")),
    "Exo-Suit Chestplate": ("外骨骼胸甲", ("нагрудник экзокостюма", "m"), ("Peto de exotraje", "m"), ("Exoanzug-Brustplatte", "f"), ("Peitoral de exotraje", "m")),
    "Exo-Suit Helmet": ("外骨骼头盔", ("шлем экзокостюма", "m"), ("Casco de exotraje", "m"), ("Exoanzug-Helm", "m"), ("Capacete de exotraje", "m")),
    "Exo-Suit Leggings": ("外骨骼护腿", ("поножи экзокостюма", "pl"), ("Grebas de exotraje", "fpl"), ("Exoanzug-Beinschutz", "m"), ("Calças de exotraje", "fpl")),
}
TIER_RE = re.compile(r"^(%s) (%s)$" % ("|".join(TIERS), "|".join(re.escape(n) for n in NOUNS)))


def noun(n, lang):
    zh, ru, es, de, pt = NOUNS[n]
    return {"zh_cn": zh, "ru_ru": ru[0][0].upper() + ru[0][1:], "es_es": es[0], "de_de": de[0], "pt_br": pt[0]}[lang]


def tier_name(text, lang):
    m = TIER_RE.match(text)
    if not m:
        return noun(text, lang) if text in NOUNS and text not in ("Circuit", "Pump") else None
    tier, n = m.group(1), m.group(2)
    zh, (ru, rg), (es, eg), (de, dg), (pt, pg) = NOUNS[n]
    if lang == "zh_cn": return ZH_TIER[tier] + zh
    if lang == "ru_ru": return f"{RU_TIER[tier]}{RU_END[rg]} {ru}"
    if lang == "es_es": return f"{es} {ES_TIER[tier][ESG[eg]]}"
    if lang == "pt_br": return f"{pt} {PT_TIER[tier][ESG[pg]]}"
    if tier == "Elite": return f"Elite-{de}"
    return f"{DE_TIER[tier]}{DE_END[dg]} {de}"


def compose(text, lang):
    return metal_name(text, lang) or tier_name(text, lang)
