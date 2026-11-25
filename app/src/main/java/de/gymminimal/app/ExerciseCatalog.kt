package de.gymminimal.app

/**
 * Übungsinfo: beanspruchte Muskeln, Ausführung und häufige Fehler für die Vorschlagsübungen.
 * Eigene, knappe Beschreibungen als Orientierung, kein Ersatz für eine Einweisung.
 *
 * Format je Zeile: Name|Ausrüstung|Hauptmuskeln|Mitbeteiligt|Schritt~Schritt~…|Fehler~Fehler~…
 * Alias: Name=Andere Übung (gleiche Info, eigener Name).
 */
data class ExerciseInfo(
    val name: String,
    val equipment: String,
    val primary: List<String>,
    val secondary: List<String>,
    val steps: List<String>,
    val errors: List<String>
)

/** Muskelbezeichnungen (Chips) → Region der Muskelkarte. Unterarme haben keine eigene Region. */
val MUSCLE_REGIONS = mapOf(
    "Brust" to "Brust",
    "Schulter vorne" to "Schulter", "Schulter seitlich" to "Schulter", "Schulter hinten" to "Schulter",
    "Bizeps" to "Bizeps", "Trizeps" to "Trizeps", "Unterarme" to "",
    "Latissimus" to "Rücken", "Oberer Rücken" to "Rücken", "Nacken" to "Nacken", "Unterer Rücken" to "Unterer Rücken",
    "Bauch" to "Bauch", "Seitliche Bauchmuskeln" to "Bauch", "Hüftbeuger" to "Oberschenkel vorne",
    "Gesäß" to "Gesäß", "Abduktoren" to "Gesäß",
    "Beinstrecker" to "Oberschenkel vorne", "Adduktoren" to "Oberschenkel vorne",
    "Beinbeuger" to "Oberschenkel hinten", "Waden" to "Waden"
)

/** Region → 2 (Haupt) / 1 (mit). */
fun regionLevels(info: ExerciseInfo): Map<String, Int> {
    val out = mutableMapOf<String, Int>()
    info.secondary.mapNotNull { MUSCLE_REGIONS[it]?.takeIf(String::isNotEmpty) }.forEach { out[it] = 1 }
    info.primary.mapNotNull { MUSCLE_REGIONS[it]?.takeIf(String::isNotEmpty) }.forEach { out[it] = 2 }
    return out
}

private const val CATALOG = """
Bankdrücken|Langhantel · Flachbank|Brust|Schulter vorne,Trizeps|Auf die Bank legen, Augen unter der Stange, Füße fest am Boden.~Schulterblätter zurück und nach unten ziehen, etwas mehr als schulterbreit greifen.~Stange kontrolliert zur unteren Brust senken, Ellbogen etwa 45° vom Körper.~Kraftvoll nach oben drücken, ohne das Gesäß von der Bank zu heben.|Stange auf der Brust abfedern lassen~Ellbogen weit seitlich abspreizen~Hohlkreuz mit abhebendem Gesäß
Bankdrücken mit Kurzhanteln|Kurzhanteln · Flachbank|Brust|Schulter vorne,Trizeps|Mit den Hanteln auf den Oberschenkeln hinsetzen und beim Zurücklegen auf Brusthöhe bringen.~Schulterblätter zurückziehen, Hanteln über der Brust, Handflächen Richtung Füße.~Hanteln seitlich der Brust absenken, bis eine leichte Dehnung spürbar ist.~Nach oben drücken, oben leicht zueinander führen.|Hanteln zu tief absenken und Schultern nach vorn rollen~Ellbogen ganz seitlich abspreizen~Hanteln oben zusammenschlagen
Schrägbankdrücken|Langhantel · Schrägbank|Brust|Schulter vorne,Trizeps|Bank auf etwa 30° stellen, Füße fest am Boden.~Schulterblätter zurückziehen, etwas mehr als schulterbreit greifen.~Stange zur oberen Brust unterhalb des Schlüsselbeins senken.~Senkrecht nach oben drücken.|Bank zu steil, dann arbeitet vor allem die Schulter~Stange zum Hals führen~Gesäß von der Bank heben
Schrägbankdrücken mit Kurzhanteln|Kurzhanteln · Schrägbank|Brust|Schulter vorne,Trizeps|Bank auf etwa 30° stellen, Hanteln auf Höhe der oberen Brust.~Schulterblätter zurückziehen, Ellbogen leicht unter Schulterhöhe.~Hanteln kontrolliert absenken bis zur Dehnung.~Nach oben drücken, ohne die Arme ganz durchzuschlagen.|Bank zu steil~Ellbogen zu weit nach außen~Mit Schwung aus der unteren Position drücken
Negativbankdrücken|Langhantel · Negativbank|Brust|Trizeps,Schulter vorne|Beine sichern, Kopf liegt tiefer als die Hüfte.~Etwas mehr als schulterbreit greifen, Schulterblätter zurückziehen.~Stange zur unteren Brust senken.~Nach oben drücken und kontrolliert wiederholen.|Ohne Partner oder Sicherung trainieren~Stange zum Hals führen~Zu steiler Winkel
Brustpresse|Maschine|Brust|Schulter vorne,Trizeps|Sitz so einstellen, dass die Griffe auf Höhe der mittleren Brust sind.~Rücken an die Lehne, Schulterblätter zurück.~Griffe nach vorn drücken, ohne die Ellbogen ganz zu strecken.~Langsam zurückführen, bis eine leichte Dehnung spürbar ist.|Sitz zu hoch oder zu tief eingestellt~Schultern nach vorn ziehen~Gewicht zurückfallen lassen
Brustpresse einarmig|Maschine|Brust|Schulter vorne,Trizeps,Seitliche Bauchmuskeln|Sitz einstellen, Griff auf Höhe der mittleren Brust.~Rumpf fest, freie Hand am Sitz.~Mit einem Arm nach vorn drücken, ohne den Oberkörper zu drehen.~Langsam zurückführen und Seite wechseln.|Oberkörper mitdrehen~Schulter nach vorn ziehen~Zu schnell zurückführen
Butterfly|Maschine|Brust|Schulter vorne|Sitz so einstellen, dass die Griffe auf Brusthöhe sind.~Arme leicht gebeugt, Rücken an der Lehne.~Griffe im Bogen vor der Brust zusammenführen.~Langsam öffnen, bis eine Dehnung in der Brust spürbar ist.|Arme stark beugen und drücken statt führen~Zu weit öffnen und die Schulter überdehnen~Schwung nutzen
Butterfly Reverse|Maschine|Schulter hinten|Oberer Rücken,Nacken|Brust an die Lehne, Griffe auf Schulterhöhe.~Arme fast gestreckt, Schultern tief.~Arme im Bogen nach hinten führen, bis sie seitlich sind.~Langsam zurück nach vorn.|Schultern hochziehen~Schulterblätter zusammenpressen statt die Arme zu führen~Zu viel Gewicht mit Schwung
Kabel-Flys|Kabelzug|Brust|Schulter vorne|Rollen auf Schulterhöhe, einen Schritt vor die Station stellen.~Arme leicht gebeugt, Oberkörper leicht vorgeneigt.~Griffe im Bogen vor der Brust zusammenführen.~Kontrolliert öffnen bis zur Dehnung.|Arme stark beugen und drücken~Hohlkreuz~Zu weit nach hinten öffnen
Kabel-Flys von oben|Kabelzug|Brust|Schulter vorne|Rollen oben einstellen, einen Schritt nach vorn.~Arme leicht gebeugt.~Griffe von oben nach unten vor die Hüfte zusammenführen.~Langsam zurück nach oben.|Oberkörper zu weit vorbeugen~Arme beugen und drücken~Schwung aus dem Rücken
Kabel-Flys von unten|Kabelzug|Brust|Schulter vorne|Rollen unten einstellen, einen Schritt nach vorn.~Arme leicht gebeugt, Handflächen nach oben.~Griffe von unten nach oben vor die obere Brust führen.~Langsam zurück nach unten.|Schultern hochziehen~Mit dem Bizeps ziehen~Zu schnell absenken
Kurzhantel-Flys|Kurzhanteln · Flachbank|Brust|Schulter vorne|Auf die Bank legen, Hanteln über der Brust, Handflächen zueinander.~Arme leicht gebeugt halten.~Hanteln im Bogen seitlich absenken bis zur Dehnung.~Im gleichen Bogen zurück nach oben.|Zu tief absenken~Arme beugen und drücken~Zu schweres Gewicht
Liegestütze|Eigengewicht|Brust|Trizeps,Schulter vorne,Bauch|Hände etwas mehr als schulterbreit, Körper bildet eine Linie.~Bauch und Gesäß anspannen.~Brust bis knapp über den Boden senken, Ellbogen etwa 45° vom Körper.~Kraftvoll hochdrücken.|Hüfte durchhängen lassen~Kopf nach vorn strecken~Nur halbe Bewegung
Liegestütze erhöht|Eigengewicht · Bank|Brust|Trizeps,Schulter vorne,Bauch|Hände auf eine Bank oder Erhöhung, Körper in einer Linie.~Bauch und Gesäß anspannen.~Brust zur Kante senken.~Hochdrücken.|Hüfte durchhängen lassen~Ellbogen weit abspreizen~Zu schnell absenken
Liegestütze eng|Eigengewicht|Trizeps|Brust,Schulter vorne,Bauch|Hände unter den Schultern, Körper in einer Linie.~Ellbogen eng am Körper führen.~Brust zum Boden senken.~Hochdrücken, bis die Arme gestreckt sind.|Ellbogen nach außen~Hüfte durchhängen lassen~Kopf hängen lassen
Dips|Barren|Brust,Trizeps|Schulter vorne|Mit gestreckten Armen auf den Barren stützen.~Oberkörper leicht vorneigen, Schultern tief.~Absenken, bis die Oberarme etwa waagerecht sind.~Hochdrücken bis zur Streckung.|Zu tief absenken~Schultern hochziehen~Mit Schwung aus den Beinen
Überzüge mit Kurzhantel|Kurzhantel · Flachbank|Brust,Latissimus|Trizeps|Quer oder längs auf die Bank, Hantel mit beiden Händen über der Brust.~Arme leicht gebeugt halten.~Hantel im Bogen hinter den Kopf senken bis zur Dehnung.~Im Bogen zurück über die Brust.|Arme dabei beugen und strecken~Hohlkreuz~Zu weit hinter den Kopf
Latziehen|Kabelzug|Latissimus|Bizeps,Schulter hinten|Oberschenkel unter dem Polster fixieren, etwas mehr als schulterbreit greifen.~Brust raus, Schultern tief.~Stange zur oberen Brust ziehen, Ellbogen nach unten.~Kontrolliert nach oben führen.|Stange hinter den Nacken ziehen~Mit dem Oberkörper weit zurücklehnen~Schultern hochziehen
Latziehen eng|Kabelzug · enger Griff|Latissimus|Bizeps,Oberer Rücken|Enger Griff, Oberschenkel fixieren.~Brust raus, leicht zurückgelehnt.~Griff zur Brust ziehen, Ellbogen nah am Körper.~Langsam nach oben führen.|Schwung aus dem Oberkörper~Schultern nach vorn rollen~Nur mit den Armen ziehen
Latziehen breit|Kabelzug · breiter Griff|Latissimus|Oberer Rücken,Bizeps|Deutlich mehr als schulterbreit greifen.~Brust raus, Schultern tief.~Stange zur oberen Brust ziehen.~Kontrolliert nach oben führen.|Hinter den Nacken ziehen~Zu weit zurücklehnen~Unvollständige Bewegung
Latziehen im Untergriff|Kabelzug · Untergriff|Latissimus|Bizeps|Schulterbreit im Untergriff greifen.~Brust raus, Schultern tief.~Stange zur Brust ziehen, Ellbogen eng.~Langsam nach oben.|Nur mit dem Bizeps ziehen~Schwung~Schultern hochziehen
Klimmzüge|Klimmzugstange|Latissimus|Bizeps,Schulter hinten,Bauch|Etwas mehr als schulterbreit im Obergriff hängen.~Schultern aktiv nach unten ziehen.~Hochziehen, bis das Kinn über der Stange ist.~Kontrolliert ganz absenken.|Schwung aus den Beinen~Nur halbe Bewegung~Schultern in Richtung Ohren
Klimmzüge im Untergriff|Klimmzugstange · Untergriff|Latissimus,Bizeps|Schulter hinten|Schulterbreit im Untergriff hängen.~Schultern nach unten ziehen.~Brust zur Stange ziehen.~Langsam ganz absenken.|Kippen und Schwung~Nicht vollständig absenken~Kopf nach vorn strecken
Klimmzüge assistiert|Maschine oder Band|Latissimus|Bizeps,Schulter hinten|Knie oder Füße auf Plattform oder Band, Gegengewicht einstellen.~Etwas mehr als schulterbreit greifen, Schultern tief.~Hochziehen, bis das Kinn über der Stange ist.~Kontrolliert absenken.|Zu viel Unterstützung dauerhaft~Schwung~Nur halbe Bewegung
Rudern am Kabel|Kabelzug sitzend|Latissimus,Oberer Rücken|Bizeps,Schulter hinten|Aufrecht sitzen, Knie leicht gebeugt.~Griff mit gestreckten Armen halten, Rücken gerade.~Griff zum Bauch ziehen, Schulterblätter zusammenführen.~Langsam nach vorn strecken.|Mit dem Oberkörper schwingen~Runder Rücken~Schultern hochziehen
Rudern am Kabel eng|Kabelzug · enger Griff|Latissimus|Oberer Rücken,Bizeps|Enger Griff, aufrecht sitzen.~Rücken gerade, Brust raus.~Griff zum Bauchnabel ziehen, Ellbogen eng.~Kontrolliert zurück.|Runder Rücken beim Zurückführen~Schwung~Nur mit den Armen ziehen
Langhantelrudern|Langhantel|Latissimus,Oberer Rücken|Bizeps,Unterer Rücken,Schulter hinten|Schulterbreit stehen, Knie leicht gebeugt, Oberkörper etwa 45° vorgeneigt.~Rücken gerade, Stange hängt unter den Schultern.~Stange zum Bauch ziehen.~Kontrolliert absenken.|Runder Rücken~Oberkörper beim Ziehen aufrichten~Zu viel Schwung
Kurzhantelrudern|Kurzhantel · Bank|Latissimus|Oberer Rücken,Bizeps|Knie und Hand auf der Bank abstützen, Rücken gerade.~Hantel hängt unter der Schulter.~Hantel zur Hüfte ziehen, Ellbogen nah am Körper.~Langsam absenken.|Oberkörper aufdrehen~Runder Rücken~Hantel zur Brust statt zur Hüfte
T-Bar-Rudern|T-Bar oder Landmine|Latissimus,Oberer Rücken|Bizeps,Unterer Rücken|Über der Stange stehen, Knie gebeugt, Oberkörper vorgeneigt.~Rücken gerade, Griffe fassen.~Zur Brust ziehen, Schulterblätter zusammen.~Kontrolliert absenken.|Runder Rücken~Mit den Beinen schwingen~Zu schweres Gewicht
Maschinenrudern|Maschine|Latissimus,Oberer Rücken|Bizeps,Schulter hinten|Brust ans Polster, Sitz so einstellen, dass die Griffe auf Brusthöhe sind.~Arme gestreckt, Schultern tief.~Griffe zum Körper ziehen, Schulterblätter zusammen.~Langsam zurück.|Brust vom Polster lösen~Schultern hochziehen~Halbe Bewegung
Rudern aufrecht|Langhantel oder SZ-Stange|Schulter seitlich,Nacken|Bizeps|Schulterbreit greifen, Stange vor den Oberschenkeln.~Rumpf fest.~Stange nah am Körper bis knapp unter die Brust ziehen, Ellbogen führen.~Langsam absenken.|Zu eng greifen~Stange bis zum Kinn ziehen~Schwung aus dem Rücken
Kreuzheben|Langhantel|Unterer Rücken,Gesäß,Beinbeuger|Beinstrecker,Oberer Rücken,Unterarme|Mittelfuß unter der Stange, hüftbreit stehen.~Schulterbreit greifen, Rücken gerade, Brust raus.~Stange nah am Körper durch Strecken von Hüfte und Knien anheben.~Kontrolliert in gleicher Bahn absetzen.|Runder Rücken~Stange vom Körper weg~Oben ins Hohlkreuz lehnen
Rumänisches Kreuzheben|Langhantel|Beinbeuger,Gesäß|Unterer Rücken|Hüftbreit stehen, Stange vor den Oberschenkeln, Knie leicht gebeugt.~Hüfte nach hinten schieben, Rücken gerade.~Stange nah an den Beinen absenken, bis die Beinrückseite dehnt.~Hüfte nach vorn bringen und aufrichten.|Runder Rücken~Knie zu stark beugen~Stange vom Körper weg
Hyperextensions|Hyperextensionbank|Unterer Rücken|Gesäß,Beinbeuger|Hüfte knapp über dem Polster, Füße fixiert.~Arme vor der Brust kreuzen.~Oberkörper mit geradem Rücken absenken.~Bis zur Linie mit den Beinen aufrichten.|Über die Linie hinaus ins Hohlkreuz~Schwung~Runder Rücken
Face Pulls|Kabelzug · Seil|Schulter hinten|Oberer Rücken,Nacken|Seil auf Augenhöhe, mit beiden Händen greifen.~Einen Schritt zurück, Arme gestreckt.~Seil zum Gesicht ziehen, Hände auseinander, Ellbogen hoch.~Langsam zurück.|Zu schweres Gewicht, Oberkörper lehnt zurück~Ellbogen zu tief~Schultern hochziehen
Gerader Armzug|Kabelzug|Latissimus|Trizeps|Vor dem oberen Kabel stehen, Stange schulterbreit greifen.~Leicht vorneigen, Arme fast gestreckt.~Stange im Bogen bis zu den Oberschenkeln ziehen.~Langsam zurück nach oben.|Arme beugen und drücken~Oberkörper mitbewegen~Schultern hochziehen
Latzug einarmig|Kabelzug|Latissimus|Bizeps|Griff mit einer Hand, seitlich oder vor dem Kabel sitzen oder knien.~Schulter tief, Arm gestreckt.~Ellbogen zur Hüfte ziehen.~Langsam zurück.|Oberkörper verdrehen~Schulter hochziehen~Nur mit dem Arm ziehen
Shrugs|Kurzhanteln oder Langhantel|Nacken|Unterarme|Aufrecht stehen, Gewicht seitlich oder vor dem Körper.~Arme gestreckt lassen.~Schultern gerade nach oben zu den Ohren ziehen.~Kurz halten, langsam absenken.|Schultern kreisen~Arme beugen~Kopf nach vorn schieben
Kettlebell-Rudern|Kettlebell|Latissimus|Oberer Rücken,Bizeps|Oberkörper vorneigen oder mit einer Hand abstützen.~Rücken gerade, Kettlebell hängt unter der Schulter.~Zur Hüfte ziehen.~Kontrolliert absenken.|Runder Rücken~Oberkörper aufdrehen~Schwung
Kniebeugen|Langhantel|Beinstrecker,Gesäß|Beinbeuger,Unterer Rücken,Bauch|Stange auf dem oberen Rücken, schulterbreit stehen, Fußspitzen leicht nach außen.~Rumpf anspannen, Blick nach vorn.~Hüfte und Knie beugen, bis die Oberschenkel mindestens waagerecht sind.~Über die ganze Fußsohle nach oben drücken.|Knie fallen nach innen~Fersen heben ab~Runder Rücken in der Tiefe
Frontkniebeugen|Langhantel|Beinstrecker|Gesäß,Bauch,Oberer Rücken|Stange vorn auf den Schultern, Ellbogen hoch.~Aufrecht stehen, Rumpf fest.~Tief beugen, Oberkörper bleibt aufrecht.~Nach oben drücken.|Ellbogen sinken ab~Oberkörper kippt nach vorn~Fersen heben ab
Goblet Squats|Kurzhantel oder Kettlebell|Beinstrecker,Gesäß|Bauch|Gewicht vor der Brust halten, schulterbreit stehen.~Rumpf fest, aufrecht.~Zwischen die Knie absenken.~Über die ganze Fußsohle hochdrücken.|Knie fallen nach innen~Runder Rücken~Fersen heben ab
Hackenschmidt-Kniebeugen|Maschine|Beinstrecker|Gesäß|Rücken an das Polster, Schultern unter die Polster, Füße schulterbreit.~Sicherung lösen.~Kontrolliert tief beugen.~Hochdrücken, ohne die Knie durchzustrecken.|Gesäß löst sich vom Polster~Knie ganz durchstrecken~Füße zu weit vorn oder hinten
Bulgarian Split Squats|Kurzhanteln · Bank|Beinstrecker,Gesäß|Adduktoren,Beinbeuger|Hinteren Fuß auf eine Bank legen, vorderer Fuß weit genug vorn.~Oberkörper aufrecht, Gewicht auf dem vorderen Bein.~Senkrecht absenken, bis das hintere Knie fast den Boden berührt.~Über die vordere Ferse hochdrücken.|Vorderes Knie fällt nach innen~Zu kurzer Abstand~Mit dem hinteren Bein drücken
Ausfallschritte|Kurzhanteln oder Eigengewicht|Beinstrecker,Gesäß|Beinbeuger,Adduktoren|Aufrecht stehen.~Großen Schritt nach vorn machen.~Absenken, bis beide Knie etwa 90° gebeugt sind.~Zurück in den Stand drücken.|Vorderes Knie fällt nach innen~Oberkörper kippt nach vorn~Zu kurzer Schritt
Gehende Ausfallschritte|Kurzhanteln oder Eigengewicht|Beinstrecker,Gesäß|Beinbeuger,Adduktoren|Aufrecht stehen, Gewicht seitlich.~Großen Schritt nach vorn, absenken.~Über das vordere Bein hochdrücken und mit dem hinteren nach vorn gehen.~Abwechselnd weiter.|Knie fällt nach innen~Schritte zu kurz~Oberkörper kippt nach vorn
Step-ups|Kurzhanteln · Kasten|Beinstrecker,Gesäß|Beinbeuger|Einen Fuß ganz auf den Kasten stellen.~Über das obere Bein nach oben drücken.~Oben aufrecht stehen.~Kontrolliert absteigen.|Mit dem unteren Bein abstoßen~Knie fällt nach innen~Kasten zu hoch
Beinpresse|Maschine|Beinstrecker,Gesäß|Beinbeuger|Rücken und Gesäß an der Lehne, Füße schulterbreit auf der Platte.~Sicherung lösen.~Knie kontrolliert zur Brust beugen, so tief es ohne Abheben des Gesäßes geht.~Platte wegdrücken, ohne die Knie ganz durchzustrecken.|Gesäß hebt vom Sitz ab~Knie ganz durchstrecken~Knie fallen nach innen
Beinpresse einbeinig|Maschine|Beinstrecker,Gesäß|Beinbeuger|Ein Fuß mittig auf der Platte, Rücken an der Lehne.~Sicherung lösen.~Knie kontrolliert beugen.~Hochdrücken, ohne das Knie zu überstrecken.|Becken verdrehen~Knie fällt nach innen~Zu schweres Gewicht
Beinstrecker|Maschine|Beinstrecker||Rücken an die Lehne, Polster vor den Schienbeinen über den Knöcheln.~Knie auf Höhe des Drehpunkts.~Beine strecken, oben kurz halten.~Langsam absenken.|Schwung~Gesäß hebt ab~Zu schnell absenken
Beinbeuger|Maschine|Beinbeuger|Waden|Maschine so einstellen, dass das Knie auf Höhe des Drehpunkts ist.~Polster oberhalb der Fersen.~Beine gegen den Widerstand beugen.~Langsam strecken.|Hüfte hebt ab~Schwung~Halbe Bewegung
Beinbeuger liegend=Beinbeuger
Beinbeuger sitzend|Maschine sitzend|Beinbeuger|Waden|Knie auf Höhe des Drehpunkts, Oberschenkelpolster fest.~Polster oberhalb der Fersen.~Beine nach unten und hinten beugen.~Langsam zurückführen.|Oberkörper mitbewegen~Schwung~Halbe Bewegung
Hip Thrusts|Langhantel · Bank|Gesäß|Beinbeuger,Beinstrecker|Oberer Rücken an der Bank, Stange gepolstert auf der Hüfte.~Füße hüftbreit, Knie etwa 90° gebeugt.~Hüfte nach oben drücken, bis Oberkörper und Oberschenkel eine Linie bilden.~Kontrolliert absenken.|Oben ins Hohlkreuz~Füße zu weit vorn~Über die Zehen drücken
Glute Bridge|Eigengewicht oder Langhantel|Gesäß|Beinbeuger|Auf dem Rücken liegen, Füße hüftbreit aufgestellt.~Bauch fest.~Hüfte nach oben drücken, Gesäß anspannen.~Langsam absenken.|Hohlkreuz statt Hüftstreckung~Knie fallen nach außen oder innen~Zu schnell
Abduktorenmaschine|Maschine|Abduktoren|Gesäß|Aufrecht sitzen, Polster außen an den Knien.~Beine nach außen drücken.~Kurz halten.~Langsam zurück.|Schwung~Oberkörper vor und zurück~Zu weit zurückfallen lassen
Adduktorenmaschine|Maschine|Adduktoren||Aufrecht sitzen, Polster innen an den Knien.~Beine zusammendrücken.~Kurz halten.~Langsam öffnen.|Zu weit öffnen~Schwung~Oberkörper mitbewegen
Wadenheben|Maschine oder Stufe|Waden||Fußballen auf der Kante, Fersen frei.~Knie fast gestreckt.~Fersen so hoch wie möglich heben.~Langsam tief absenken.|Wippen statt ganzer Bewegung~Knie beugen und mitdrücken~Zu schnell
Wadenheben sitzend|Maschine sitzend|Waden||Fußballen auf der Kante, Polster auf den Oberschenkeln.~Sicherung lösen.~Fersen hoch heben.~Langsam tief absenken.|Halbe Bewegung~Schwung~Zu schnell
Wadenheben stehend=Wadenheben
Sumo-Kniebeugen|Kurzhantel oder Kettlebell|Beinstrecker,Adduktoren,Gesäß|Bauch|Breit stehen, Fußspitzen nach außen.~Gewicht hängt zwischen den Beinen.~Aufrecht absenken, Knie zeigen über die Füße.~Hochdrücken.|Knie fallen nach innen~Runder Rücken~Fersen heben ab
Nordic Hamstring Curls|Eigengewicht · fixierte Füße|Beinbeuger|Gesäß|Kniend, Fersen fixiert, Körper aufrecht.~Hüfte gestreckt halten.~So langsam wie möglich nach vorn absenken.~Mit den Händen abfangen und zurückdrücken.|Hüfte abknicken~Zu schnell fallen lassen~Ohne Polster unter den Knien
Good Mornings|Langhantel|Beinbeuger,Unterer Rücken|Gesäß|Stange auf dem oberen Rücken, hüftbreit stehen.~Knie leicht gebeugt.~Hüfte nach hinten schieben, Oberkörper mit geradem Rücken vorneigen.~Aufrichten.|Runder Rücken~Zu schweres Gewicht~Knie stark beugen
Kickbacks am Kabel|Kabelzug · Fußmanschette|Gesäß|Beinbeuger|Manschette am Fuß, vor der Station stehen und festhalten.~Rumpf fest, Standbein leicht gebeugt.~Bein gestreckt nach hinten führen.~Langsam zurück.|Hohlkreuz~Oberkörper schwingt~Zu weit nach hinten
Schulterdrücken|Langhantel|Schulter vorne|Trizeps,Schulter seitlich,Oberer Rücken|Stehend oder sitzend, Stange vor dem Hals auf den Schultern.~Rumpf und Gesäß anspannen.~Stange senkrecht über den Kopf drücken.~Kontrolliert zurück.|Hohlkreuz~Stange vor dem Körper statt über dem Kopf~Schwung aus den Beinen
Kurzhantel-Schulterdrücken|Kurzhanteln|Schulter vorne,Schulter seitlich|Trizeps|Sitzend, Hanteln auf Schulterhöhe.~Rücken an der Lehne, Rumpf fest.~Hanteln über den Kopf drücken.~Langsam auf Schulterhöhe senken.|Hohlkreuz~Hanteln zu tief absenken~Hanteln oben zusammenschlagen
Arnold Press|Kurzhanteln|Schulter vorne,Schulter seitlich|Trizeps|Sitzend, Hanteln vor der Brust, Handflächen zum Körper.~Beim Drücken die Hände nach außen drehen.~Über dem Kopf strecken, Handflächen nach vorn.~Die Bewegung rückwärts zurück.|Schwung~Hohlkreuz~Drehung zu schnell
Military Press|Langhantel stehend|Schulter vorne|Trizeps,Schulter seitlich,Bauch|Stehend, Füße zusammen oder hüftbreit, Stange auf den vorderen Schultern.~Gesäß und Bauch fest.~Stange senkrecht über den Kopf drücken, Kopf nach dem Passieren nach vorn.~Kontrolliert zurück.|Hohlkreuz~Schwung aus den Knien~Stange vor dem Körper
Seitheben|Kurzhanteln|Schulter seitlich|Nacken|Aufrecht stehen, Hanteln seitlich.~Arme leicht gebeugt.~Seitlich bis auf Schulterhöhe heben.~Langsam absenken.|Schultern hochziehen~Schwung~Über Schulterhöhe heben
Seitheben am Kabel|Kabelzug|Schulter seitlich|Nacken|Seitlich zur Station, Griff in der äußeren Hand.~Arm leicht gebeugt.~Seitlich bis Schulterhöhe heben.~Langsam zurück.|Oberkörper neigen~Schultern hochziehen~Schwung
Seitheben an der Maschine|Maschine|Schulter seitlich|Nacken|Sitz einstellen, Polster außen an den Oberarmen.~Schultern tief.~Arme seitlich bis Schulterhöhe heben.~Langsam absenken.|Schultern hochziehen~Schwung~Zu weit heben
Frontheben|Kurzhanteln oder Scheibe|Schulter vorne||Aufrecht stehen, Gewicht vor den Oberschenkeln.~Arme fast gestreckt.~Vor dem Körper bis Schulterhöhe heben.~Langsam absenken.|Schwung aus dem Rücken~Über Kopfhöhe heben~Hohlkreuz
Reverse Flys|Kurzhanteln|Schulter hinten|Oberer Rücken|Oberkörper weit vorneigen, Rücken gerade.~Hanteln hängen, Arme leicht gebeugt.~Seitlich nach außen bis auf Schulterhöhe heben.~Langsam absenken.|Schwung~Schultern hochziehen~Runder Rücken
Reverse Flys am Kabel|Kabelzug|Schulter hinten|Oberer Rücken|Kabel gekreuzt greifen, Rollen auf Schulterhöhe.~Arme leicht gebeugt.~Arme seitlich nach hinten führen.~Langsam zurück.|Schultern hochziehen~Zu schweres Gewicht~Oberkörper zurücklehnen
Vorgebeugtes Seitheben=Reverse Flys
Schulterdrücken an der Maschine|Maschine|Schulter vorne|Trizeps,Schulter seitlich|Sitz so einstellen, dass die Griffe auf Schulterhöhe sind.~Rücken an der Lehne.~Griffe nach oben drücken.~Langsam zurück.|Hohlkreuz~Ellbogen ganz durchstrecken~Gewicht zurückfallen lassen
Bizepscurls|Kurzhanteln oder Langhantel|Bizeps|Unterarme|Aufrecht stehen, Ellbogen am Körper.~Handflächen nach vorn.~Gewicht durch Beugen der Ellbogen heben.~Langsam ganz absenken.|Schwung aus dem Rücken~Ellbogen wandern nach vorn~Nicht ganz absenken
Langhantelcurls|Langhantel oder SZ-Stange|Bizeps|Unterarme|Schulterbreit im Untergriff, Ellbogen am Körper.~Rumpf fest.~Stange zur Brust beugen.~Langsam ganz absenken.|Schwung~Ellbogen nach vorn~Hohlkreuz
Kurzhantelcurls=Bizepscurls
Hammer Curls|Kurzhanteln|Bizeps,Unterarme||Hanteln seitlich, Handflächen zueinander.~Ellbogen am Körper.~Hanteln mit neutralem Griff heben.~Langsam absenken.|Schwung~Ellbogen wandern~Handgelenke abknicken
Scottcurls|Scottbank|Bizeps||Oberarme ganz auf dem Polster, Untergriff.~Arme fast gestreckt.~Gewicht zur Schulter beugen.~Langsam absenken, ohne die Arme zu überstrecken.|Gesäß hebt ab, Schwung~Zu schnell absenken~Unten ganz durchstrecken
Preacher Curls=Scottcurls
Konzentrationscurls|Kurzhantel|Bizeps||Sitzend, Ellbogen innen am Oberschenkel.~Arm hängt gestreckt.~Hantel zur Schulter beugen.~Langsam absenken.|Oberkörper mitbewegen~Ellbogen vom Bein lösen~Zu schnell
Kabelcurls|Kabelzug|Bizeps|Unterarme|Vor dem unteren Kabel stehen, Untergriff.~Ellbogen am Körper.~Griff zur Brust beugen.~Langsam strecken.|Schwung~Ellbogen nach vorn~Oberkörper zurücklehnen
Curls am Kabel=Kabelcurls
Incline Curls|Kurzhanteln · Schrägbank|Bizeps||Auf der Schrägbank zurücklehnen, Arme hängen.~Ellbogen bleiben hinter dem Körper.~Hanteln zur Schulter beugen.~Langsam ganz absenken.|Ellbogen nach vorn~Schwung~Schultern nach vorn rollen
Reverse Curls|Langhantel oder SZ-Stange|Unterarme,Bizeps||Obergriff, schulterbreit.~Ellbogen am Körper.~Stange zur Brust beugen.~Langsam absenken.|Handgelenke abknicken~Schwung~Zu schweres Gewicht
Zottman Curls|Kurzhanteln|Bizeps,Unterarme||Hanteln im Untergriff zur Schulter beugen.~Oben die Hände in den Obergriff drehen.~Im Obergriff langsam absenken.~Unten zurückdrehen.|Schwung~Zu schnell absenken~Ellbogen wandern
Trizepsdrücken|Kabelzug · Stange|Trizeps||Vor dem oberen Kabel stehen, Ellbogen am Körper.~Stange auf Brusthöhe.~Stange nach unten drücken, bis die Arme gestreckt sind.~Langsam zurück.|Ellbogen wandern nach vorn~Oberkörper drückt mit~Schultern hochziehen
Trizepsdrücken am Seil|Kabelzug · Seil|Trizeps||Seil greifen, Ellbogen am Körper.~Nach unten drücken.~Unten die Seilenden auseinanderziehen.~Langsam zurück.|Ellbogen wandern~Oberkörper vorbeugen und drücken~Schwung
Trizepsdrücken einarmig|Kabelzug|Trizeps||Griff mit einer Hand, Ellbogen am Körper.~Arm nach unten strecken.~Kurz halten.~Langsam zurück.|Ellbogen wandert~Schulter hochziehen~Oberkörper drehen
French Press|SZ-Stange · Flachbank|Trizeps||Auf der Bank liegen, Stange über der Brust.~Oberarme senkrecht halten.~Stange durch Beugen der Ellbogen Richtung Stirn senken.~Wieder strecken.|Ellbogen nach außen~Oberarme bewegen mit~Zu schweres Gewicht
Skull Crushers=French Press
Overhead-Trizepsstrecken|Kurzhantel oder Kabel|Trizeps||Gewicht mit beiden Händen über dem Kopf.~Ellbogen nah am Kopf.~Gewicht hinter den Kopf senken.~Arme strecken.|Ellbogen nach außen~Hohlkreuz~Zu tief und schnell
Trizeps-Kickbacks|Kurzhantel|Trizeps||Oberkörper vorneigen, Oberarm am Körper und waagerecht.~Unterarm hängt nach unten.~Arm nach hinten strecken.~Langsam zurück.|Oberarm bewegt sich mit~Schwung~Runder Rücken
Enges Bankdrücken|Langhantel · Flachbank|Trizeps|Brust,Schulter vorne|Schulterbreit greifen.~Ellbogen nah am Körper.~Stange zur unteren Brust senken.~Hochdrücken.|Zu eng greifen~Ellbogen nach außen~Abfedern
Unterarmcurls|Kurzhanteln oder Langhantel|Unterarme||Unterarme auf Bank oder Oberschenkel, Hände frei.~Untergriff.~Handgelenke nach oben beugen.~Langsam absenken.|Unterarme heben ab~Zu große Last~Schwung
Crunches|Eigengewicht|Bauch||Auf dem Rücken liegen, Knie gebeugt.~Hände an den Schläfen oder vor der Brust.~Oberkörper einrollen, bis die Schulterblätter abheben.~Langsam zurück.|Am Kopf ziehen~Schwung~Ganz aufsetzen
Crunches am Kabel|Kabelzug · Seil|Bauch||Vor dem oberen Kabel knien, Seil neben dem Kopf.~Hüfte bleibt ruhig.~Oberkörper nach unten einrollen.~Langsam zurück.|Aus der Hüfte beugen~Mit den Armen ziehen~Zu schweres Gewicht
Reverse Crunches|Eigengewicht|Bauch|Hüftbeuger|Auf dem Rücken, Beine angewinkelt angehoben.~Hände neben dem Körper.~Becken einrollen und vom Boden heben.~Langsam absenken.|Schwung~Beine schwingen statt Becken heben~Zu schnell
Plank|Eigengewicht|Bauch|Schulter vorne,Gesäß|Unterarme unter den Schultern.~Körper bildet eine gerade Linie.~Bauch und Gesäß anspannen.~Position halten und ruhig atmen.|Hüfte hängt durch~Gesäß zu hoch~Luft anhalten
Side Plank|Eigengewicht|Seitliche Bauchmuskeln|Schulter seitlich,Gesäß|Seitlich auf einem Unterarm, Füße übereinander.~Hüfte anheben, Körper in einer Linie.~Spannung halten.~Seite wechseln.|Hüfte sinkt ab~Schulter fällt ein~Körper kippt nach vorn
Sit-ups|Eigengewicht|Bauch|Hüftbeuger|Auf dem Rücken, Knie gebeugt, Füße am Boden.~Rücken einrollen.~Bis zum Sitzen aufrichten.~Langsam abrollen.|Am Kopf ziehen~Schwung~Hohlkreuz beim Ablegen
Beinheben|Eigengewicht|Bauch|Hüftbeuger|Auf dem Rücken, Hände neben dem Gesäß.~Beine gestreckt.~Beine bis senkrecht heben.~Langsam absenken, ohne Hohlkreuz.|Hohlkreuz~Schwung~Beine fallen lassen
Hängendes Beinheben|Klimmzugstange|Bauch|Hüftbeuger,Unterarme|An der Stange hängen, Schultern aktiv.~Beine gestreckt.~Beine bis mindestens waagerecht heben, Becken leicht einrollen.~Langsam absenken.|Schwingen~Nur aus der Hüfte heben~Zu schnell
Hängendes Knieheben|Klimmzugstange|Bauch|Hüftbeuger|An der Stange hängen, Schultern aktiv.~Knie zur Brust ziehen.~Becken leicht einrollen.~Langsam absenken.|Schwingen~Halbe Bewegung~Schultern hängen lassen
Russian Twists|Eigengewicht oder Scheibe|Seitliche Bauchmuskeln|Bauch|Sitzen, Oberkörper leicht zurückgelehnt.~Rücken gerade, Füße am Boden oder angehoben.~Oberkörper zur Seite drehen.~Abwechselnd wiederholen.|Runder Rücken~Nur die Arme bewegen~Zu schnell
Dead Bug|Eigengewicht|Bauch||Rückenlage, Arme senkrecht, Knie 90° angehoben.~Unteren Rücken am Boden halten.~Gegenüberliegenden Arm und Bein strecken.~Zurück und Seite wechseln.|Hohlkreuz~Zu schnell~Luft anhalten
Bicycle Crunches|Eigengewicht|Bauch,Seitliche Bauchmuskeln||Rückenlage, Hände an den Schläfen.~Schultern leicht angehoben.~Ellbogen zum gegenüberliegenden Knie, anderes Bein strecken.~Abwechselnd.|Am Kopf ziehen~Zu schnell~Nur die Ellbogen bewegen
Ab Wheel Rollout|Bauchroller|Bauch|Latissimus,Schulter vorne|Kniend, Roller unter den Schultern.~Bauch fest, Becken leicht eingerollt.~Kontrolliert nach vorn rollen.~Mit dem Bauch zurückziehen.|Hohlkreuz~Zu weit rollen~Mit der Hüfte zurück statt mit dem Bauch
Bauchpresse|Maschine|Bauch||Sitz einstellen, Polster auf der Brust oder Griffe fassen.~Rücken an der Lehne.~Oberkörper nach vorn einrollen.~Langsam zurück.|Mit den Armen ziehen~Schwung~Zu schweres Gewicht
Hollow Hold|Eigengewicht|Bauch|Hüftbeuger|Rückenlage, unterer Rücken am Boden.~Arme und Beine gestreckt anheben.~Schultern vom Boden lösen.~Position halten.|Hohlkreuz~Kopf nach hinten~Zu tief halten, wenn der Rücken abhebt
Mountain Climbers|Eigengewicht|Bauch|Hüftbeuger,Schulter vorne|Liegestützposition, Hände unter den Schultern.~Rumpf fest.~Knie abwechselnd zur Brust ziehen.~Zügig wechseln.|Gesäß zu hoch~Hüfte hängt durch~Schultern hinter den Händen
Pallof Press|Kabelzug|Seitliche Bauchmuskeln|Bauch|Seitlich zum Kabel, Griff vor der Brust.~Rumpf fest, Hüfte gerade.~Griff gerade nach vorn drücken, ohne zu drehen.~Halten und zurück.|Oberkörper dreht mit~Zu nah an der Station~Hohlkreuz
Farmer's Walk|Kurzhanteln oder Kettlebells|Unterarme,Nacken|Bauch,Gesäß|Schwere Gewichte seitlich greifen.~Aufrecht, Schultern tief.~Mit kurzen Schritten gehen.~Kontrolliert abstellen.|Schultern hängen lassen~Runder Rücken~Zu lange Schritte
Australian Pull-ups|Stange oder Ringe · Eigengewicht|Oberer Rücken,Latissimus|Bizeps|Unter einer hüfthohen Stange hängen, Körper gestreckt.~Fersen am Boden.~Brust zur Stange ziehen.~Langsam absenken.|Hüfte hängt durch~Halbe Bewegung~Schultern hochziehen
Ring Rows|Ringe · Eigengewicht|Oberer Rücken,Latissimus|Bizeps|Ringe greifen, Körper gestreckt schräg nach hinten.~Rumpf fest.~Brust zu den Ringen ziehen.~Langsam zurück.|Hüfte hängt durch~Schwung~Schultern nach vorn
Negative Klimmzüge|Klimmzugstange|Latissimus|Bizeps|Mit Hilfe eines Kastens in die obere Position.~Kinn über der Stange.~So langsam wie möglich absenken.~Unten absteigen und wiederholen.|Fallen lassen~Schultern hochziehen~Zu wenig Kontrolle
Archer Pull-ups|Klimmzugstange|Latissimus|Bizeps,Schulter hinten|Breit greifen.~Zu einer Hand ziehen, der andere Arm bleibt gestreckt.~Langsam absenken.~Seite wechseln.|Schwung~Halbe Bewegung~Gestreckten Arm beugen
Typewriter Pull-ups|Klimmzugstange|Latissimus|Bizeps|Breit greifen, hochziehen.~Oben seitlich zu einer Hand verschieben.~Zur anderen Seite verschieben.~Absenken.|Oben absinken~Schwung~Zu schnell
Scapula Pull-ups|Klimmzugstange|Oberer Rücken,Latissimus||Mit gestreckten Armen hängen.~Nur die Schulterblätter nach unten und hinten ziehen.~Kurz halten.~Langsam zurück in den Hang.|Arme beugen~Schwung~Zu schnell
Muscle-ups|Stange oder Ringe|Latissimus,Trizeps|Brust,Bauch|Explosiv hochziehen, Brust zur Stange.~Oben über die Stange kippen.~In den Stütz drücken.~Kontrolliert zurück.|Ohne sicheren Klimmzug versuchen~Nur mit Schwung~Ellbogen nacheinander statt gleichzeitig
Dead Hang|Klimmzugstange|Unterarme|Latissimus,Schulter vorne|Schulterbreit greifen.~Gestreckt hängen.~Schultern leicht aktiv.~Halten.|Schultern ganz hängen lassen bei Beschwerden~Schwingen~Zu lange bei Schmerzen
Front Lever|Stange oder Ringe|Latissimus,Bauch|Schulter hinten|Gestreckt hängen.~Arme gestreckt, Körper waagerecht nach vorn heben.~Körper in einer Linie halten.~Kontrolliert absenken.|Hüfte abknicken~Arme beugen~Ohne Vorstufen versuchen
Back Lever|Ringe oder Stange|Latissimus,Brust|Schulter vorne,Bizeps|Über einen Skin the Cat in die Rückenposition.~Körper waagerecht, Arme gestreckt.~Halten.~Kontrolliert zurück.|Ohne Vorbereitung der Schulter~Arme beugen~Hohlkreuz
Skin the Cat|Ringe oder Stange|Schulter vorne,Latissimus|Bauch|Hängen, Beine durch die Arme ziehen.~Langsam nach hinten drehen.~Nur so weit, wie die Schulter es zulässt.~Zurück.|Zu weit drehen~Schwung~Ohne Aufwärmen
Pike Push-ups|Eigengewicht|Schulter vorne|Trizeps|Hüfte hoch, Körper als umgedrehtes V.~Hände schulterbreit.~Kopf zwischen die Hände senken.~Hochdrücken.|Hüfte sinkt~Ellbogen nach außen~Kopf zuerst statt Stirn vor die Hände
Handstand Push-ups|Eigengewicht · Wand|Schulter vorne|Trizeps,Nacken|Handstand an der Wand.~Kopf kontrolliert zum Boden senken.~Kopf vor die Hände, Dreieck bilden.~Hochdrücken.|Hohlkreuz~Auf den Kopf fallen lassen~Ohne Pike Push-ups als Vorstufe
Handstand|Eigengewicht|Schulter vorne|Bauch,Trizeps|Hände schulterbreit, in den Handstand schwingen.~Arme gestreckt, Schultern zu den Ohren schieben.~Körper in einer Linie halten.~Seitlich abrollen oder zurück.|Hohlkreuz~Gebeugte Arme~Ohne sicheres Abrollen
Archer Push-ups|Eigengewicht|Brust|Trizeps,Schulter vorne|Hände sehr breit.~Zu einer Seite absenken, der andere Arm bleibt gestreckt.~Hochdrücken.~Seite wechseln.|Hüfte hängt durch~Gestreckten Arm beugen~Halbe Bewegung
Diamond Push-ups|Eigengewicht|Trizeps|Brust|Hände unter der Brust, Daumen und Zeigefinger bilden eine Raute.~Körper in einer Linie.~Brust zu den Händen senken.~Hochdrücken.|Ellbogen weit nach außen~Hüfte hängt durch~Halbe Bewegung
Decline Push-ups|Eigengewicht · Erhöhung|Brust|Schulter vorne,Trizeps|Füße auf einer Erhöhung, Hände am Boden.~Körper in einer Linie.~Brust absenken.~Hochdrücken.|Hüfte hängt durch~Kopf nach vorn~Zu hohe Erhöhung
Clap Push-ups|Eigengewicht|Brust|Trizeps,Schulter vorne|Liegestützposition.~Explosiv hochdrücken, bis die Hände abheben.~Klatschen.~Weich mit gebeugten Armen landen.|Hart mit gestreckten Armen landen~Hüfte hängt durch~Ermüdet weitermachen
Pseudo Planche Push-ups|Eigengewicht|Brust,Schulter vorne|Trizeps,Bauch|Hände auf Hüfthöhe, Finger nach außen oder hinten.~Körper nach vorn verlagern.~Absenken, Ellbogen nah am Körper.~Hochdrücken, Vorlage halten.|Vorlage verlieren~Hüfte hängt durch~Handgelenke nicht aufgewärmt
Planche Lean|Eigengewicht|Schulter vorne|Bauch,Trizeps|Liegestützposition, Hände auf Hüfthöhe.~Arme gestreckt.~Schultern weit vor die Hände schieben.~Halten.|Arme beugen~Hohlkreuz~Zu weit lehnen ohne Kontrolle
Tuck Planche|Eigengewicht|Schulter vorne|Bauch,Trizeps|Hände schulterbreit, Arme gestreckt.~Knie zur Brust ziehen.~Füße vom Boden lösen, Körper vorlehnen.~Halten.|Arme beugen~Rücken rund ohne Spannung~Handgelenke nicht aufgewärmt
Ring Dips|Ringe|Brust,Trizeps|Schulter vorne,Bauch|Im Stütz auf den Ringen, Ringe nah am Körper.~Absenken, bis die Oberarme etwa waagerecht sind.~Ringe ruhig halten.~Hochdrücken und oben nach außen drehen.|Ringe wandern auseinander~Zu tief~Schultern nach vorn rollen
Bank-Dips|Bank|Trizeps|Brust,Schulter vorne|Hände hinter dem Körper auf die Bankkante.~Beine nach vorn, Gesäß nah an der Bank.~Absenken, bis die Oberarme etwa waagerecht sind.~Hochdrücken.|Zu tief~Gesäß weit von der Bank~Schultern hochziehen
Straight Bar Dips|Stange|Trizeps,Brust|Schulter vorne|Im Stütz auf einer geraden Stange.~Oberkörper leicht vorneigen.~Absenken, bis die Stange die Brust fast berührt.~Hochdrücken.|Zu tief~Schultern hochziehen~Schwung
Pistol Squats|Eigengewicht|Beinstrecker,Gesäß|Bauch,Adduktoren|Auf einem Bein stehen, anderes Bein nach vorn.~Arme nach vorn für Gleichgewicht.~Auf einem Bein tief absenken.~Hochdrücken.|Knie fällt nach innen~Ferse hebt ab~Ohne Vorstufen versuchen
Shrimp Squats|Eigengewicht|Beinstrecker,Gesäß||Auf einem Bein stehen, hinteren Fuß hinter dem Körper halten.~Absenken, bis das hintere Knie fast den Boden berührt.~Hochdrücken.~Seite wechseln.|Knie fällt nach innen~Oberkörper fällt nach vorn~Zu schnell
Sissy Squats|Eigengewicht|Beinstrecker||Aufrecht, an einer Stange festhalten.~Auf die Zehen gehen.~Knie nach vorn schieben, Oberkörper nach hinten lehnen.~Zurück aufrichten.|Hüfte knickt ab~Zu tief ohne Gewöhnung~Ohne Halt
Cossack Squats|Eigengewicht|Adduktoren,Beinstrecker|Gesäß|Sehr breit stehen.~Gewicht auf ein Bein verlagern und tief absenken.~Anderes Bein bleibt gestreckt, Zehen nach oben.~Seite wechseln.|Ferse hebt ab~Runder Rücken~Zu tief ohne Beweglichkeit
Jump Squats|Eigengewicht|Beinstrecker,Gesäß|Waden|Schulterbreit stehen.~In die Kniebeuge gehen.~Explosiv hochspringen.~Weich landen und direkt weiter.|Harte Landung~Knie fallen nach innen~Ermüdet weitermachen
L-Sit|Barren oder Boden|Bauch,Hüftbeuger|Trizeps|Im Stütz, Arme gestreckt.~Schultern tief.~Beine gestreckt waagerecht nach vorn heben.~Halten.|Schultern hochziehen~Knie beugen~Runder Rücken
Dragon Flag|Bank|Bauch|Latissimus|Auf der Bank liegen, hinter dem Kopf festhalten.~Körper gestreckt anheben, nur die Schultern liegen auf.~Langsam als Linie absenken.~Wieder anheben.|Hüfte abknicken~Zu schnell absenken~Hohlkreuz
Toes to Bar|Klimmzugstange|Bauch|Hüftbeuger,Latissimus|An der Stange hängen, Schultern aktiv.~Beine gestreckt zur Stange heben.~Zehen berühren die Stange.~Kontrolliert absenken.|Schwingen~Knie stark beugen~Nur Schwung
Windshield Wipers|Klimmzugstange oder Boden|Seitliche Bauchmuskeln|Bauch|Beine senkrecht nach oben.~Rumpf fest.~Beine kontrolliert zu einer Seite senken.~Über die Mitte zur anderen Seite.|Schwung~Zu weit senken~Schultern heben ab
Burpees|Eigengewicht|Beinstrecker,Brust|Schulter vorne,Bauch|In die Hocke, Hände auf den Boden.~Beine nach hinten in die Liegestützposition springen.~Liegestütz, Beine zurück zur Hocke.~Hochspringen.|Hüfte hängt durch~Harte Landung~Runder Rücken beim Aufrichten
"""

/** Alle Einträge, Aliasse aufgelöst. Schlüssel = normalisierter Name. */
val EXERCISE_INFOS: Map<String, ExerciseInfo> by lazy {
    val base = linkedMapOf<String, ExerciseInfo>()
    val aliases = mutableListOf<Pair<String, String>>()
    CATALOG.trim().lines().map(String::trim).filter(String::isNotEmpty).forEach { line ->
        if ('|' !in line && '=' in line) {
            val (a, b) = line.split('=', limit = 2); aliases += a.trim() to b.trim(); return@forEach
        }
        val p = line.split('|')
        require(p.size == 6) { "Übungsinfo fehlerhaft: $line" }
        fun list(s: String) = s.split(',').map(String::trim).filter(String::isNotEmpty)
        base[exerciseNameKey(p[0])] = ExerciseInfo(p[0].trim(), p[1].trim(), list(p[2]), list(p[3]),
            p[4].split('~').map(String::trim), p[5].split('~').map(String::trim))
    }
    aliases.forEach { (a, b) -> base[exerciseNameKey(b)]?.let { base[exerciseNameKey(a)] = it.copy(name = a) } }
    base
}

fun exerciseInfo(name: String): ExerciseInfo? = EXERCISE_INFOS[exerciseNameKey(name)]
