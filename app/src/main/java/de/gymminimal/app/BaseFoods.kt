package de.gymminimal.app

/*
 * Grundliste: gerundete Richtwerte je 100 g (bzw. 100 ml bei Getränken), angelehnt an USDA FoodData Central
 * und übliche Packungsangaben. Kohlenhydrate ohne Ballaststoffe (EU-Kennzeichnung).
 * Spalten: Name; kcal; Protein; Fett; Kohlenhydrate; Ballaststoffe; Zucker; Salz; Portionen (Label:Gramm|…)
 * IDs sind stabil ("b:" + normalisierter Name) und dürfen sich nicht ändern, sonst gehen Verlauf und Favoriten verloren.
 */
private val BASE_G = """
Haferflocken;372;13.5;7;58.7;10;0.7;0;1 EL:10|1 Portion:50
Reis, roh;351;7;0.6;78;1.4;0.2;0;1 Portion:75
Reis, gekocht;130;2.7;0.3;28;0.4;0.1;0;1 Portion:200
Vollkornreis, roh;350;7.5;2.7;72;3.5;0.8;0;1 Portion:75
Nudeln, roh;357;12.5;1.5;72;3;3;0;1 Portion:100
Nudeln, gekocht;150;5.5;0.9;30;1.8;0.6;0;1 Portion:250
Vollkornnudeln, roh;350;14;2.5;64;8;3;0;1 Portion:100
Couscous, roh;360;12.5;1.5;72;5;0.5;0;1 Portion:70
Bulgur, roh;342;12;1.3;63;12;0.4;0;1 Portion:70
Quinoa, roh;368;14;6;60;7;0;0;1 Portion:70
Weizenmehl Type 405;340;10;1;71;4;0.5;0;1 EL:10
Kartoffeln, gekocht;75;1.9;0.1;16;1.8;0.8;0;1 Kartoffel:80
Süßkartoffel, roh;86;1.6;0.1;17;3;4.2;0.1;1 Stück:200
Pommes frites;290;3.4;15;35;3;0.3;0.5;1 Portion:150
Vollkornbrot;220;7.5;1.5;41;7.5;3;1.1;1 Scheibe:50
Mischbrot;230;7;1.2;46;5;2;1.2;1 Scheibe:45
Toastbrot;260;8;3.5;48;3;5;1.2;1 Scheibe:25
Brötchen, Weizen;270;9;1.5;54;3;2;1.3;1 Brötchen:55
Vollkornbrötchen;250;9.5;3;44;6.5;2;1.2;1 Brötchen:60
Knäckebrot;365;9.5;1.5;72;15;2;1;1 Scheibe:10
Reiswaffeln;385;8;3;80;4;0.5;0.1;1 Waffel:8
Wrap, Weizentortilla;310;8.5;7;51;3;3;1.3;1 Wrap:62
Cornflakes;375;7;0.9;84;3;8;1.8;1 Portion:30
Müsli ohne Zuckerzusatz;365;10;7;60;9;15;0.1;1 Portion:50
Hähnchenbrust, roh;110;23;1.2;0;0;0;0.15;1 Filet:180
Hähnchenbrust, gebraten;165;31;3.6;0;0;0;0.2;1 Filet:130
Hähnchenschenkel mit Haut, roh;200;17.5;14.5;0;0;0;0.2;1 Schenkel:250
Putenbrust, roh;105;24;1;0;0;0;0.15;1 Schnitzel:150
Rinderhackfleisch, roh;250;18;20;0;0;0;0.2;1 Packung:500
Rinderhackfleisch mager, roh;125;21;5;0;0;0;0.2;1 Packung:400
Gemischtes Hackfleisch, roh;260;17;21;0;0;0;0.2;1 Packung:500
Rindersteak, Hüfte, roh;120;22;3.5;0;0;0;0.15;1 Steak:200
Schweinefilet, roh;110;21;2.5;0;0;0;0.15
Schweineschnitzel, roh;105;22;2;0;0;0;0.15;1 Schnitzel:150
Kochschinken;105;20;3;1;0;1;2.2;1 Scheibe:20
Putenaufschnitt;100;20;1.5;1.5;0;1;2;1 Scheibe:15
Salami;380;23;32;1;0;0.5;4.4;1 Scheibe:8
Lachs, roh;200;20;13;0;0;0;0.15;1 Filet:125
Räucherlachs;180;21;10.5;0;0;0;3;1 Scheibe:20
Thunfisch im eigenen Saft, abgetropft;110;25;1;0;0;0;0.9;1 Dose:150
Kabeljau, roh;80;18;0.8;0;0;0;0.2;1 Filet:125
Garnelen, roh;85;19;1;0;0;0;0.6
Ei;140;12.5;10;0.7;0;0.7;0.35;1 Ei (M):55
Eiklar;50;11;0.2;0.7;0;0.7;0.45;1 Eiklar:33
Tofu natur;130;13.5;7.5;1.5;0.5;0.5;0;1 Packung:200
Linsen, roh;330;24;1.5;48;17;2;0;1 Portion:70
Linsen, gekocht;105;9;0.4;12;8;1.8;0;1 Portion:200
Kichererbsen, Dose abgetropft;120;7;2.5;15;6;0.5;0.6;1 Dose:240
Kidneybohnen, Dose abgetropft;100;7;0.5;13;6.5;1;0.5;1 Dose:250
Proteinpulver, Whey;380;75;6;7;0;5;0.5;1 Messlöffel:30
Magerquark;67;12;0.3;4;0;4;0.1;1 Becher:250|1 Packung:500
Speisequark 20 %;110;12.5;5;3.5;0;3.5;0.1;1 Becher:250
Skyr natur;63;11;0.2;4;0;4;0.1;1 Becher:150|1 Packung:450
Naturjoghurt 3,5 %;65;4;3.5;4.5;0;4.5;0.15;1 Becher:150
Naturjoghurt 1,5 %;48;4;1.5;4.5;0;4.5;0.15;1 Becher:150
Griechischer Joghurt 10 %;130;5;10;4;0;4;0.1;1 Portion:150
Hüttenkäse;98;12.5;4.5;2;0;2;0.8;1 Becher:200
Gouda;356;25;28;0;0;0;1.8;1 Scheibe:25
Mozzarella;240;18;18;1;0;1;0.5;1 Kugel:125
Feta;265;17;21;0.5;0;0.5;3;1 Packung:200
Parmesan;400;32;29;0;0;0;1.6;1 EL gerieben:7
Frischkäse natur;240;6;23;3;0;3;0.8;1 EL:20
Butter;740;0.7;82;0.6;0;0.6;0;1 TL:5|1 EL:12
Sahne 30 %;290;2.4;30;3.2;0;3.2;0.1;1 EL:15
Brokkoli;34;2.8;0.4;4;2.6;1.7;0.1;1 Portion:150
Blumenkohl;25;2;0.3;3;2.5;2;0.05;1 Portion:150
Karotten;36;0.9;0.2;7;2.8;4.7;0.15;1 Karotte:80
Tomaten;18;0.9;0.2;2.6;1.2;2.6;0;1 Tomate:80
Gurke;15;0.6;0.1;2.2;0.5;1.7;0;1 Gurke:400
Paprika, rot;31;1;0.3;4.6;2.1;4.2;0;1 Paprika:160
Zucchini;17;1.2;0.3;2.2;1;1.7;0;1 Zucchini:250
Spinat, roh;23;2.9;0.4;1.1;2.2;0.4;0.2;1 Portion:100
Kopfsalat;14;0.9;0.1;1.8;1.2;1.8;0;1 Portion:50
Zwiebel;40;1.1;0.1;7.6;1.7;4.2;0;1 Zwiebel:80
Champignons;22;3.1;0.3;0.6;2;0.5;0;1 Portion:100
Erbsen, TK;70;5.4;0.4;9.5;5;4;0;1 Portion:150
Mais, Dose abgetropft;85;3;1.2;14;3;4;0.5;1 Dose:140
Grüne Bohnen;29;1.8;0.2;3.7;2.7;3.3;0;1 Portion:150
Avocado;200;2;19.5;1.9;6.7;0.7;0;1 Avocado:150
Banane;93;1.1;0.3;20;2;17;0;1 Banane:120
Apfel;54;0.3;0.2;12;2.2;10;0;1 Apfel:150
Orange;47;1;0.2;9;2;8.5;0;1 Orange:150
Erdbeeren;32;0.7;0.3;5.5;2;4.9;0;1 Portion:150
Heidelbeeren;57;0.7;0.3;12;2.4;10;0;1 Portion:125
Weintrauben;70;0.7;0.3;16;1;16;0;1 Portion:125
Kiwi;60;1;0.5;11;3;9;0;1 Kiwi:75
Mango;62;0.8;0.4;13;1.6;13;0;1 Mango:300
Ananas;52;0.5;0.1;12;1.4;10;0;1 Scheibe:80
Datteln, getrocknet;290;2;0.4;66;8;63;0;1 Dattel:8
Rosinen;300;3;0.5;68;4;60;0;1 EL:15
Olivenöl;900;0;100;0;0;0;0;1 TL:5|1 EL:10
Rapsöl;900;0;100;0;0;0;0;1 TL:5|1 EL:10
Erdnussbutter;620;25;50;13;7;5;1;1 EL:15
Mandeln;595;21;50;9;12.5;4.4;0;1 Handvoll:25
Walnüsse;685;15;65;7;6.7;2.6;0;1 Handvoll:25
Cashewkerne;585;18;44;27;3.3;6;0;1 Handvoll:25
Erdnüsse, geröstet;610;25;49;13;8;4;0;1 Handvoll:25
Zartbitterschokolade 70 %;570;9;42;33;11;28;0;1 Rippe:10
Nuss-Nougat-Creme;540;6;31;57;0;56;0.1;1 TL:10
Zucker;400;0;0;100;0;100;0;1 TL:5
Honig;320;0.4;0;80;0;80;0;1 TL:8
Konfitüre;250;0.3;0.1;60;1;58;0;1 TL:10
Ketchup;100;1.2;0.1;23;0.5;22;1.8;1 EL:15
Mayonnaise;700;1.1;77;2.6;0;1.8;1.1;1 EL:15
Pizza Margherita, TK;230;9;8.5;29;2;3;1.1;1 Pizza:350
""".trimIndent()

private val BASE_ML = """
Milch 1,5 %;47;3.4;1.5;4.8;0;4.8;0.1;1 Glas:250
Milch 3,5 %;64;3.3;3.5;4.8;0;4.8;0.1;1 Glas:250
Haferdrink;45;0.5;1.5;7;0.8;4;0.1;1 Glas:250
Orangensaft;43;0.7;0.1;9;0.2;8.5;0;1 Glas:250
Cola;42;0;0;10.6;0;10.6;0;1 Dose:330
Bier, Pils;42;0.5;0;3;0;0;0;1 Flasche:500
""".trimIndent()

private fun parseBase(table: String, ml: Boolean): List<Food> = table.lines().filter { it.isNotBlank() }.map { line ->
    val c = line.split(';')
    val n = c.subList(1, 8).map { it.toDouble() }
    val portions = c.getOrNull(8).orEmpty().split('|').filter { it.contains(':') }.map {
        val (label, grams) = it.split(':'); FoodPortion(label, grams.toDouble())
    }
    Food("b:" + normalizeFood(c[0]).replace(Regex("[^a-z0-9]+"), "-").trim('-'), c[0],
        Nutrients(n[0], n[1], n[2], n[3], n[4], n[5], n[6]), ml, portions)
}

val BASE_FOODS: List<Food> by lazy { (parseBase(BASE_G, false) + parseBase(BASE_ML, true)).sortedBy { normalizeFood(it.name) } }
