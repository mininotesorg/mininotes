<!-- Translated from README.md, SHA-256 afc0641c40eed2999c2ac47e7f27c64cd554ed024330bd7b8c9cbe20b18a265d -->

# Mininotes

Mininotes ist ein Notizbuch für dein Telefon und deinen PC, das deine Notizen, Ordner und Dateien auf all deinen Geräten
gleich hält und sie mit den Menschen teilt, die du wählst. Es gibt keinen Server, kein Konto und niemanden dazwischen:
Alles reist Ende zu Ende versiegelt über das Minima-Netzwerk oder direkt von Gerät zu Gerät im selben WLAN.

[![Neueste Version](https://img.shields.io/github/v/release/mininotesorg/mininotes?label=latest%20build)](https://github.com/mininotesorg/mininotes/releases/latest)

<!-- not in the app -->

[中文](README.zh.md) · [Español](README.es.md) · [Français](README.fr.md) · [English](README.md) ·
[Português](README.pt.md) · [Italiano](README.it.md) · [日本語](README.ja.md) · [Русский](README.ru.md) ·
[हिन्दी](README.hi.md) · [فارسی](README.fa.md)

## Inhalt

1. [Neuigkeiten](#1-neuigkeiten)
2. [Installieren](#2-installieren)
3. [Schreiben](#3-schreiben)
4. [Ordnen: Start, Ordner, Dock und Favoriten](#4-ordnen-start-ordner-dock-und-favoriten)
5. [Deine eigenen Geräte](#5-deine-eigenen-geräte)
6. [Mit anderen teilen](#6-mit-anderen-teilen)
7. [Personen und Geräte, und Mein Profil](#7-personen-und-geräte-und-mein-profil)
8. [Wissen, wo alles steht](#8-wissen-wo-alles-steht)
9. [Dateien, Fotos und Aufnahmen](#9-dateien-fotos-und-aufnahmen)
10. [Dateien, die auf deinem PC bleiben](#10-dateien-die-auf-deinem-pc-bleiben)
11. [Versionen, Archiv, Papierkorb und Sicherungen](#11-versionen-archiv-papierkorb-und-sicherungen)
12. [Der Agent: fragen und erledigen lassen](#12-der-agent-fragen-und-erledigen-lassen)
13. [Auf Minima stempeln mit Integritas](#13-auf-minima-stempeln-mit-integritas)
14. [Privatsphäre, Verschlüsselung und Sicherheit](#14-privatsphäre-verschlüsselung-und-sicherheit)
15. [Private Bereiche und der Hilferuf](#15-private-bereiche-und-der-hilferuf)
16. [Sprachen und Übersetzung](#16-sprachen-und-übersetzung)
17. [Jeder Menüeintrag in einer Zeile](#17-jeder-menüeintrag-in-einer-zeile)
18. [Aktualisierungen](#18-aktualisierungen)
19. [Sag etwas dazu](#19-sag-etwas-dazu)
20. [Selbst bauen](#20-selbst-bauen)
21. [Was in diesem Repository ist](#21-was-in-diesem-repository-ist)
22. [Lizenz](#22-lizenz)

---

## 1. Neuigkeiten

Das Neueste zuerst. Jede Version ist auf Telefon und PC gleich, sofern nichts anderes dasteht.

### 0.4.028

- Eine gestempelte Notiz oder Datei, die du teilst, nimmt ihren Stempel mit: Die Person, mit der du sie teilst, sieht
  ihn in *Auf Minima stempeln…*, als Stempel dessen, was sie bekommen hat, nur wenn es genau dasselbe ist, mit den
  Links, um ihn im Minima Explorer zu prüfen. Ein Stempel, der nach dem Teilen gemacht wird, erreicht sie auch.
- Wenn du eine Datei teilst, deren Integritas-Bericht neben ihr behalten wird, sendet *Auch den Integritas-Bericht
  senden* den Bericht mit.
- Das Sprachfenster beim ersten Start ist auf Englisch, mit den Sprachen in alphabetischer Reihenfolge.

### 0.4.024 bis 0.4.027

- **Auf Minima stempeln…** ist eine einzige Zeile im Menü einer Notiz oder einer Datei, die sagt, wie weit ihr Stempel
  ist, und öffnet ein einziges Fenster mit allem: stempeln, ihn auf Minima suchen, erneut prüfen, die früher
  gestempelten Versionen und eine Datei mit einem Fingerabdruck vergleichen. Der PDF-Bericht von Integritas lässt sich
  an die Notiz anhängen oder neben der Datei behalten.
- Stempel gehen an deine anderen Geräte: Was auf dem Telefon gestempelt wurde, steht auf dem PC in der Liste, und
  umgekehrt.
- Eine gestempelte Datei trägt einen kleinen Stempel auf ihrem Symbol.
- *Mit einem Fingerabdruck vergleichen…*: Füge den SHA-256 ein, den du für eine Datei bekommen hast, und sieh sofort, ob
  es dieselbe Datei ist. Nichts verlässt das Gerät, und kein Konto ist nötig.
- Was Integritas antwortet, wird in deiner Sprache gesagt, und wenn es nicht sofort antwortet, wird es erneut gefragt.
- Auf dem PC sind die Wörter jedes Bildschirms, jeder Taste und jedes Hinweises in deiner Sprache; auch die ersten
  Ordner eines neuen Notizbuchs werden darin benannt. Auf Französisch trägt eine neue Notiz *nouv.*
- Das Telefon hört nicht mehr auf zu reagieren, wenn sein Bildschirm neu gezeichnet wird, während es die Relays noch zu
  erreichen versucht.

### 0.4.021 bis 0.4.023

- Die Tabs einer Notiz bekommen eine andere Reihenfolge, indem du sie auf dem PC ziehst oder auf dem Telefon gedrückt
  hältst und verschiebst; ihr Menü hat auch *Nach links* und *Nach rechts*.
- Die Sprache wird bei jedem Start gefragt, bis eine bestätigt ist; auf dem Telefon genügt ein Tippen auf eine Sprache.
- Jede App stellt die andere vor: *Mininotes auf deinem Handy…* auf dem PC, *Mininotes auf deinem PC…* auf dem Telefon,
  mit einem Code zum Scannen.
- Das Windows-Installationsprogramm zeigt das Mininotes-Logo und kann Mininotes starten, wenn es fertig ist.
- Eine in Temp begonnene Notiz bleibt in Temp, auch wenn der Bildschirm vor ihrem ersten Wort neu gezeichnet wird.
- Geräte sagen einander nicht mehr immer wieder, dass sie eine Datei haben, was sie beschäftigt hielt.

### 0.4.019 bis 0.4.020

- Eine neue Installation fragt zuerst nach ihrer Sprache, und ihr README und ihre erste Notiz sind darin geschrieben.
  README gibt es in elf Sprachen.
- Auf dem PC legt Strg+V auf Start, in einem Ordner oder in Temp das Bild oder die Dateien aus der Zwischenablage dort
  ab.
- Auf dem PC ein Installationsprogramm (MSI): keine Administratorrechte, Mininotes in Einstellungen, Apps, und
  *Mininotes deinstallieren…* im Menü, das eine Sicherung anbietet und dich deine Notizen behalten lässt oder nicht.
  Nach 30 Tagen ohne Sicherung erinnert Mininotes dich daran.
- Auf dem PC werden Zeilen mit Fett, Kursiv oder Links nicht mehr übereinander gezeichnet.
- Auf dem Telefon wird, was ankommt, während Mininotes geschlossen ist, in einer einzigen Benachrichtigung gesammelt.
- *Auf Minima gestempelt…* listet alles Gestempelte auf, mit einem +, um noch etwas zu stempeln.

### 0.4.016 bis 0.4.018

- Der Stempel einer Notiz umfasst ihren Titel ebenso wie ihren Text.
- Temp steht in Zuletzt, wenn du es benutzt.
- Aus anderen Apps eingefügter Text behält seine Zeilenumbrüche.
- Ein Bild, das in Temp gelegt wird, erreicht deine anderen Geräte früher.

### 0.4.011 bis 0.4.015

- Auf dem Telefon, Einstellungen, *Tastatur lernt aus meinen Notizen*: eingeschaltet funktioniert die Spracheingabe der
  Tastatur in Notizen.
- Der Integritas-Schlüssel geht an die Geräte, die du wählst. Das Menü *Diese Notiz* einer Notiz hat *Auf Minima
  stempeln…*, eine gestempelte Datei ist markiert, und die Details eines Stempels verlinken zum Minima Explorer.
- Weniger Arbeit bei jedem Start: Geräte wiederholen einander nicht mehr alles.
- Das Telefon hört nach einem Neustart oder einem Update von selbst wieder zu.

### 0.4.010

- Aus *Lies mich* wird **README**: auf Start und auf GitHub ein und derselbe Text, dieser hier. Ein README, das niemand
  geändert hat, wird von selbst neu geschrieben, wenn eine neue Version ein neues mitbringt; eines, in das du
  geschrieben hast, bleibt so, wie du es gemacht hast.

### 0.4.008 bis 0.4.009

- Ein Albumordner **Fotos** standardmäßig auf Start: deine Bilder in einem Raster zum Durchsehen, jedes groß geöffnet,
  mit vorherigem und nächstem.
- Ein Suchergebnis öffnet sich beim ersten Treffer, mit einer Suchleiste, um durch jeden Treffer der Notiz zu gehen.
- Das Gespräch mit dem **Agent** nutzt das ganze Fenster, mit einer Vollbildtaste, und bietet an, dich an die richtige
  Stelle zu bringen, zum Beispiel zum Integritas-Teil von *Mein Profil*. Es behält das Gespräch einige Stunden.
- *Lies mich* wird über *Auf Start zeigen* ein- oder ausgeblendet, mit eigenem Symbol.
- *Lies mich* beschreibt den Notfallkontakt (den Hilferuf) und die Grenzen privater Bereiche.

### 0.4.007

- Eine Notiz, die auf einem deiner Geräte aus einem geteilten Ordner verschoben wird, synchronisiert weiter mit deinen
  anderen Geräten.
- Weniger Hin und Her zwischen Geräten: Dieselbe Antwort wird nicht immer wieder gesendet, so arbeitet ein Telefon
  weniger hart.
- Auf dem PC sagt der Mauszeiger auf einem Kreis neben dem Titel einer Notiz, wer das ist, was die Person mit der Notiz
  darf, wie weit sie mit dem Geschriebenen ist und wie viele Dateien noch zu ihr unterwegs sind.

### 0.4.006

- Die Karte eines PDFs zeigt den oberen Teil seiner ersten Seite; die Karte einer Word-, PowerPoint-, Excel-,
  OpenDocument-, RTF-, HTML- oder Textdatei zeigt ihre ersten Zeilen.
- Auf dem Telefon hakt *Mehrere auswählen…* auf einer gehaltenen Datei mehrere Dateien ab, um sie zusammen mit einer
  anderen App zu senden oder in den Papierkorb zu legen.
- *Mein Profil* nimmt den Integritas-API-Schlüssel direkt entgegen, mit *Speichern*, und kann ihn an eigene Geräte
  weitergeben, die du einschaltest. *Auf Minima gestempelt…* ist ins Menü gewandert.
- Auf dem PC hat die Textgröße von *Mein Profil* dieselbe Leiter aus A− und A+ wie eine Notiz, und ein Rechtsklick in
  Zuletzt bietet *Aus Zuletzt entfernen*. Der Name der App steht links in der Leiste, neben dem Logo.
- Die Worte, die mit deinem Link gesendet werden, sind in der Sprache, die du gewählt hast.
- Lange Beschreibungen unter dem Mauszeiger werden ganz gezeigt, auf mehreren Zeilen.

### 0.4.005

- Eine Datei einer Notiz, die in den Papierkorb kommt, wird auf jedem Gerät und für alle, die die Notiz bekommen, aus
  der Notiz genommen. *Zurücklegen* bringt sie überall zurück.
- Was auf das Senden wartet, zählt nur die Dateien, die die Notiz zeigt, und listet jede davon auf.
- *Mein Profil* hat einen Integritas-Teil, mit deinem Konto und der Liste dessen, was du gestempelt hast.
- Auf dem PC hat jeder Menüeintrag eine kurze Beschreibung unter dem Mauszeiger, ein- oder ausschaltbar in *Mein
  Profil*.
- Auf dem Telefon legt *Kopieren* auf einer gehaltenen Datei die Datei selbst in die Zwischenablage; *Mit einer anderen
  App senden…* trägt das übliche Teilen-Zeichen.
- Die Einstellungen einer Gruppe liegen hinter ihrem ›; *Mein Profil* hat *Verwalten…* für Personen und Geräte; jeder
  Eintrag, der ein Fenster oder eine Seite öffnet, endet mit „…“; aus *Als Datei behalten* wird *Auf meinem Gerät
  speichern…*.
- Der Hilferuf hinterlässt in keinem Menü eine Spur: Er wird nur durch einen Code gesetzt, der in eine private Notiz
  getippt wird.
- Schreiben in einer viel genutzten geteilten Notiz auf dem PC ruckelt nicht mehr.

### 0.4.004

- *Personen und Geräte* und *Mein Profil* (früher *Profil*) stehen am Ende von *Synchronisieren und Teilen*.
- Auf dem PC stellt *Mein Profil* die Textgröße der ganzen App ein, in fünf Stufen, ab dem nächsten Start.
- *Auf meinem Computer speichern…* (auf dem Telefon *Auf meinem Telefon speichern…*) ersetzt *Herunterladen* und *Kopie
  speichern…*. Jedes Fenster, das auf dem PC eine Datei oder einen Ordner auswählt, ist das von Windows selbst.
- Der Hilferuf ist nur noch in privaten Bereichen zu finden.

### 0.4.003

- **Auf Minima stempeln** und **Auf Minima prüfen** über dein eigenes Integritas-Konto.

### 0.4.002

- Auf dem PC legt *Kopieren* im Menü einer Datei ein Bild als Bild in die Zwischenablage und jede Datei als Datei.
- Auf dem Telefon zeigt eine Zeile, die sich zu einer Auswahl aufklappt, ihren Wert rechts, auf einer Zeile.

### 0.4.001

- Auf dem PC hakt das Menü der Büroklammer die Dateien einer Notiz ab, um sie zusammen zu bearbeiten, und hat *Foto
  aufnehmen* und *Audio aufnehmen*.
- *Einen Ordner dieses PCs synchronisieren…* ist zurück in *Synchronisieren und Teilen*: ein Ordner des PCs, dessen
  Dateien darin bleiben, in Mininotes mit eigenem Zeichen gezeigt.
- Jede Meldung, die ein Fehler zeigen kann, ist übersetzt.

## 2. Installieren

**Android** (9 oder neuer): Lade `Mininotes-<version>.apk` aus
[der neuesten Version](https://github.com/mininotesorg/mininotes/releases/latest) herunter, prüfe, dass es die
veröffentlichte Datei ist, und öffne sie auf dem Telefon:

```sh
sha256sum -c Mininotes-<version>.apk.sha256
```

Android fragt, ob die Installation aus der Quelle erlaubt sein soll, von der du heruntergeladen hast; diese Berechtigung
lässt sich danach wieder abschalten.

**Prüfe, wer sie signiert hat.** Jede Release-APK ist mit demselben Mininotes-Schlüssel signiert. Ihr
SHA-256-Zertifikatsfingerabdruck, so wie [AppVerifier](https://github.com/soupslurpr/AppVerifier) ihn annimmt (kopiere
die beiden Zeilen, dann *Verify from clipboard*):

```
org.mininotes.android
F7:91:C2:14:D9:ED:9B:E9:DE:EC:6F:15:BE:D0:25:38:35:22:DD:F9:A8:21:23:4A:73:90:C8:1B:60:1F:91:AB
```

Oder mit dem Android SDK: `apksigner verify --print-certs Mininotes-<version>.apk`. Jeder andere Fingerabdruck ist kein
Build aus den Releases dieses Repositorys.

**Windows** (10 oder 11): Lade `Mininotes-Windows-<version>.msi` aus derselben Version herunter und öffne es. Es braucht
keine Administratorrechte, und Mininotes steht danach in den Windows-Einstellungen unter Apps, wie jede App.
Deinstallierst du es dort, bleiben deine Notizen erhalten, und eine spätere Installation findet sie wieder;
*Mininotes deinstallieren…* im Menü der App kann sie auch löschen, nachdem eine Sicherung angeboten wurde. Java ist
schon dabei; sonst muss nichts installiert werden.

Oder, für alle, die das lieber mögen, `Mininotes-Windows-<version>.zip`: Entpacke es irgendwo und öffne `Mininotes.exe`.

Auf beiden braucht es sonst nichts: Der Maxima-Knoten, der deine Notizen trägt, steckt in der App.

<!-- in the app again -->

## 3. Schreiben

- **Eine linierte Seite, bereit.** Eine Notiz öffnet sich mit dem Cursor darin und speichert sich beim Schreiben. Es
  gibt keine Speichern-Taste.
- **Tabs.** Eine Notiz kann mehrere Seiten haben, wie Karteikarten: *Tab hinzufügen*. Zieh einen Tab, um ihn zu
  verschieben.
- **Fett, kursiv und unterstrichen.** Setz die Zeichen um die Wörter, `**bold**`, `*italic*` und `__underline__`, oder
  drück Strg+B, Strg+I oder Strg+U.
- **Zwei Personen in einer Notiz.** Schreiben zwei in dieselbe Notiz, bevor einer den anderen gesehen hat, wird beides
  Zeile für Zeile zusammengeführt, und nichts davon geht verloren. Was jede Person schrieb, kann in ihrer Farbe
  erscheinen: Tippe auf einen Kreis unter dem Titel, um eine zu ändern.
- **Aus anderen Apps.** Unter Android steht Mininotes im Teilen-Menü: ein Screenshot, Fotos, Dateien, Text oder ein Link
  kommen in eine neue oder eine zuletzt benutzte Notiz.

## 4. Ordnen: Start, Ordner, Dock und Favoriten

- **Start** zeigt deine Notizen und Ordner als Symbole, auf so vielen Seiten du willst, jedes an seinem Platz.
- **Etwas anlegen.** Tippe auf + für eine neue Notiz, einen neuen Ordner oder Dateien von diesem oder einem anderen
  Gerät.
- **Sein Menü.** Halte ein Symbol für sein Menü: Farbe, Symbol, verschieben, teilen und mehr. Auf dem PC per
  Rechtsklick; dort führt das Logo zu Start, und mit dem Trenner änderst du die Breite der Seitenliste oder klappst sie
  ein.
- **Ordner** enthalten Notizen und andere Ordner, so tief du willst. Zieh etwas auf einen Ordner, um es hineinzulegen.
- **Verschieben.** Halte und zieh ein Symbol: in einen Ordner, aus einem heraus oder auf eine andere Seite. Beim Ziehen
  kannst du es über die Leiste oben direkt auf einen Favoriten, Temp, Archiv, Papierkorb oder Teilen ziehen, ohne Menü.
- **Seiten.** Wische zwischen den Seiten. Zieh Start mit zwei Fingern zusammen oder wähle *Alle Seiten* im Menü, um alle
  auf einmal zu sehen.
- **Farben und Symbole.** Jede Notiz und jeder Ordner kann über sein Menü eigene Farbe und eigenes Symbol oder Bild
  haben. Jede Farbe hat ihre eigene Stärke, eingestellt mit dem Regler darunter. Die Farbe von Start färbt die ganze
  App.
- **Das Dock** ist die Reihe unten auf Start: Was du dort hinlegst, ist einen Tipp entfernt. *Zum Dock hinzufügen* steht
  im Menü jeder Notiz und jedes Ordners, außer bei Favoriten. Es fasst beliebig viele und scrollt seitlich, wenn sie
  nicht passen. Jedes Gerät hat sein eigenes Dock, das nie an andere Geräte geht.
- **Favoriten** halten an einem Ort, wozu du zurückkehrst. Ein neuer Favorit kommt auch ins Dock; ein entfernter bleibt
  im Dock.
- **Zuletzt** listet, was du zuletzt geöffnet hast: an der Seite auf dem PC, auf Start auf dem Telefon. *Aus Zuletzt
  entfernen* (Rechtsklick auf dem PC) nimmt etwas von der Liste; nichts wird gelöscht. Auf dem Telefon wisch vom Dock
  nach oben oder tippe auf den Griff darüber, um Zuletzt als Karten durchzublättern: Tippe auf eine Karte zum Öffnen,
  wisch sie weg oder drück Zurück zum Schließen.
- **Temp** ist für das, was du nur eine Weile brauchst, und um Dinge an deine eigenen Geräte und die Menschen deiner
  Wahl weiterzugeben: Was darin liegt, geht an sie und ist überall weg, wenn seine Zeit um ist.
- **Mit mir geteilt** enthält, was andere mit dir teilen, bis du antwortest (siehe [Mit anderen teilen](#6-mit-anderen-teilen)).
- **Suchen** unten auf Start findet Notizen, Ordner und Dateien über ihre Wörter. **Baum** zeigt alles auf einmal.
- **Archiv und Papierkorb**: aus dem Weg oder auf dem Weg nach draußen. Aus beiden lässt sich zurücklegen (siehe
  [Versionen, Archiv, Papierkorb und Sicherungen](#11-versionen-archiv-papierkorb-und-sicherungen)).
- **README** auf Start ist dieser Text, derselbe wie auf GitHub, ohne was nur dort zählt (Inhalt, Neuigkeiten,
  Installieren, Bauen und die Liste aller Menüeinträge, die der PC unter dem Mauszeiger nennt).
- **Auf Start zeigen**, in *Einstellungen*, wählt, welche Orte Start zeigt (Zuletzt, Temp, Mit mir geteilt, Archiv,
  Papierkorb) und ob das README dort ist. Was ausgeschaltet ist, bleibt im Menü.
- **Fotos**: standardmäßig ein Albumordner auf Start, alle Bilder darin im Raster, jedes
  groß zu öffnen, das nächste und vorherige einen Wisch oder Pfeil entfernt. Füge Fotos mit seinem + hinzu oder nimm
  eines auf. Jeder Ordner lässt sich über sein Menü als Album zeigen.

## 5. Deine eigenen Geräte

Verknüpfe dein Telefon und deinen PC einmal (*Personen und Geräte*, dann *Mein Gerät hinzufügen*, und scanne den Code
des einen mit dem anderen). Ab dann bleibt, was du mit deinen Geräten teilst, auf allen von selbst aktuell, in beide
Richtungen, wenige Sekunden nach dem Schreiben. Soll ein Gerät von dir nur ansehen, stell es auf *Kann lesen*. Jedes
Gerät behält sein eigenes Layout: Wo du eine Notiz auf einem hinlegst, ändert nichts auf dem anderen, und jedes hat sein
eigenes Dock.

## 6. Mit anderen teilen

- **Jemanden hinzufügen**: Scanne den Code der Person oder öffne ihren Link; sie macht dasselbe mit deinem.
- **Eine Notiz, einen Ordner oder eine Datei teilen** über das Menü, *Synchronisieren und Teilen*, dann *Teilen…*. Wähle
  zuerst, was sie dürfen, dann eine Person oder Gruppe, oder zeig deinen Code. Jede Person bekommt eine Rolle:
  *Eigentümer* (wer es angelegt hat), *Admin*, *Kann schreiben* oder *Kann lesen*, jederzeit im Auswahlfeld daneben zu
  ändern oder zu entfernen.
- **Von einem anderen Gerät…** auf jedem + nimmt an, was dir jemand zeigt.
- **Gruppen**, etwa Freunde oder Kollegen, fassen Personen zusammen: Teile mit einer Gruppe, und alle darin bekommen
  dieselbe Rolle, auch später hinzugefügte.
- **Annehmen oder ablehnen.** Teilt jemand etwas mit dir, sagt dir eine Mitteilung Bescheid, und es wartet in *Mit mir
  geteilt*. Du siehst, was es ist und von wem, und wählst, wohin es kommt, bevor es sonst irgendwo ist. *Annehmen*, um
  es zu behalten, oder *Ablehnen*, still oder mit einer Antwort an die Person.
- **Farben reisen mit.** Die Farbe einer geteilten Notiz reist mit ihr, und jedes Gerät stellt ihre Stärke selbst ein.
- **Alles in einem geteilten Ordner ist geteilt.** Was du hineinlegst, geht an die Personen, mit denen du ihn geteilt
  hast.
- **Die Kreise** neben dem Titel einer Notiz sind die Personen und Geräte, die sie haben, jede mit Bild oder Initiale.
  Ein Tipp zeigt das Bild groß. Auf dem PC sagt der Mauszeiger auf einem Kreis, wer das ist, was die Person mit der
  Notiz darf, wie weit sie mit dem Geschriebenen ist und wie viele Dateien noch zu ihr unterwegs sind.

## 7. Personen und Geräte, und Mein Profil

Beide stehen im Menü am Ende von *Synchronisieren und Teilen*.

- **Personen und Geräte** listet die Personen, die du hinzugefügt hast, deine eigenen Geräte und deine Gruppen, jedes
  auf einer eigenen Seite. Die Seite einer Person zeigt, was du mit ihr teilst, und gibt ihr auf Wunsch ein Bild, das
  nur du siehst. Die Einstellungen einer Gruppe liegen hinter ihrem ›.
- **Das + bei Personen** verbindet dich mit jemandem: Scanne den Code, füge den Link ein oder sende mit *Meinen Link
  teilen* deinen per Nachricht oder Chat. **Das + bei Meine Geräte** fügt ein weiteres eigenes Gerät hinzu.
- **Mein Profil** enthält, was dir gehört:
  - deinen **Namen** und dein **Bild**, die an die Personen gehen, mit denen du gekoppelt bist;
  - deine **Parlons!-Adresse**, die von selbst an deine Kontakte geht;
  - deinen Code und deinen Link, damit andere dich hinzufügen;
  - den **Integritas**-API-Schlüssel (siehe [Auf Minima stempeln mit Integritas](#13-auf-minima-stempeln-mit-integritas));
  - auf dem PC die **Textgröße**: eine Leiter aus A− und A+ mit fünf Stufen, von Kleiner bis Am größten, für die ganze
    App (Start, Zuletzt, Menüs, Fenster), ab dem nächsten Start, mit *Jetzt neu starten*;
  - auf dem PC **Beschreiben, was jedes Element tut**, standardmäßig an: schaltet die kurze Zeile unter dem Mauszeiger
    auf einem Menüeintrag oder Kreis ein oder aus;
  - *Verwalten…*, das Personen und Geräte öffnet.

## 8. Wissen, wo alles steht

- **Das Zeichen** an jeder Notiz, jedem Ordner und Symbol sagt, wo es steht: ein leerer grauer Ring, nur auf diesem
  Gerät; ein gelber Pfeil, wartet aufs Senden (drück ihn, um jetzt zu senden und jede wartende Notiz und Datei zu
  sehen); drei gelbe Punkte, gesendet und wartet auf Antwort; ein grüner Haken, alle haben diese Version; zwei graue
  Balken, pausiert; ein rotes Ausrufezeichen, ein Gerät war über drei Tage nicht erreichbar.
- **Neu** listet, was sich seit deinem letzten Blick geändert hat: Notizen, in die andere schrieben, mit dir Geteiltes,
  angekommene Dateien, jeweils mit Absender, Zeit und Ort.
- **Die Leiste oben** erscheint, wenn mit einem Kontakt etwas nicht stimmt (ein Gerät seit Tagen nicht erreicht,
  gesperrt oder mit älterem Mininotes). Mininotes versucht es von selbst weiter, und *Was tun* sagt, was du tun kannst.

## 9. Dateien, Fotos und Aufnahmen

- **Anhängen**: jede Datei an eine Notiz oder einen Ordner, mehrere auf einmal.
- **Foto aufnehmen** aus einer Notiz: die Telefonkamera oder auf dem PC die Kamera-App von Windows, deren Fotos direkt
  in die Notiz kommen.
- **Audio aufnehmen** in eine Notiz.
- **Leicht zu tragen.** Fotos werden beim Hinzufügen verkleinert, so reist auch eine Notiz mit fünfzig Fotos leicht;
  abschaltbar in *Einstellungen*, *Bilder und Dateien*. Andere Dateien werden gepackt, um Platz zu sparen.
- **Vorschauen.** Die Karte eines PDFs zeigt seine erste Seite; die einer Word-, PowerPoint-, Excel-, OpenDocument-,
  RTF-, HTML- oder Textdatei ihre ersten Zeilen. Mininotes zeichnet sie aus der Datei selbst, nie Windows oder eine
  andere App, so bleibt kein Bild davon außerhalb.
- **Öffnen**: eine Datei in dem Programm, das dein Computer dafür nutzt (etwa *In LibreOffice öffnen*), oder ein anderes
  wählen mit *Öffnen mit…*. Dort Gespeichertes kommt zurück in Mininotes und geht an deine Geräte.
- **Kopieren** auf einer Datei (Rechtsklick auf dem PC, Halten auf dem Telefon) legt die Datei selbst in die
  Zwischenablage, ein Bild als Bild, zum Einfügen in einer anderen App ohne vorheriges Speichern.
- **Nach außen speichern.** *Auf meinem Computer speichern…* (auf dem Telefon *Auf meinem Telefon speichern…*) speichert
  eine Kopie, wo du willst. *Auf meinem Gerät speichern…* legt sie zusätzlich als Datei in einen Ordner dieses Geräts.
- **Mit einer anderen App senden…** auf dem Telefon öffnet das Teilen-Menü des Telefons, um eine Datei etwa per WhatsApp
  zu senden. *Mehrere auswählen…* auf einer gehaltenen Datei hakt mehrere ab, um sie so zusammen zu senden oder in den
  Papierkorb zu legen.
- **Mehrere auf einmal auf dem PC.** Das Menü der Büroklammer hat *Datei anhängen*, *Foto aufnehmen* und *Audio
  aufnehmen*, dann die Dateien der Notiz, jede mit Kästchen: *Alles auswählen*, dann *Auf meinem Computer speichern…*
  (mehrere kommen in einen Ordner), *Auf Minima stempeln…* oder *In den Papierkorb* für die abgehakten.
- **Dateien senden**: direkt an ein anderes deiner Geräte oder an eine hinzugefügte Person, ohne Notiz, von Tür zu Tür
  im selben WLAN. Was ankommt, landet auf Start, als *neu* markiert, bis du es öffnest.
- **Auf Minima stempeln…** (im Menü einer Datei oder Notiz): siehe [Auf Minima stempeln mit Integritas](#13-auf-minima-stempeln-mit-integritas).

## 10. Dateien, die auf deinem PC bleiben

- **Datei über Mininotes teilen** (auf dem PC, in *Synchronisieren und Teilen*): Eine Tabelle oder ein Dokument bleibt,
  wo es auf deinem PC ist, und öffnet sich in seinem Programm. Bei jedem Speichern geht diese Version an deine anderen
  Geräte, und eine anderswo gespeicherte wird hineingeschrieben. Die ganze Datei reist, in der neuesten Version:
  Änderungen in einer Tabelle werden nicht zusammengeführt. Hat sie sich an zwei Stellen geändert, bevor sie sich
  trafen, fragt Mininotes, welche Version bleibt, und ersetzt nichts vor deiner Antwort.
- **Einen Ordner dieses PCs synchronisieren** (auf dem PC, in *Synchronisieren und Teilen*): Wähle einen Ordner, auf C:
  oder anderswo, und alles darin erscheint in Mininotes als Ordner, markiert als auf deinem PC zu Hause. Dateien, die du
  hineinlegst, im Explorer oder aus einem Programm, werden Sekunden später übernommen und gehen an deine Geräte und an
  alle, mit denen du den Ordner teilst; Dateien von ihnen werden hineingeschrieben. Die Dateien bleiben in diesem Ordner
  auf deinem PC, außerhalb der App. Was auf der Festplatte gelöscht wird, wird nie in Mininotes gelöscht, und umgekehrt;
  hat sich eine Datei an beiden Stellen geändert, fragt Mininotes, welche Version bleibt. Auf einem Telefon liegen die
  Dateien des Ordners in der App.
- **Notizen als Dateien.** Eine Notiz oder ein Ordner kann auch als Markdown-Dateien in einem Ordner deiner Wahl auf dem
  PC liegen (*Auf meinem Gerät speichern…*). Mininotes löscht diese Dateien nie, und eine private Notiz wird nie
  hinausgeschrieben.
- Ein solcher Ordner zeigt auf seiner Kachel einen Ordner mit Pfeilen. Sein Menü sagt, wo er auf dem PC ist, dann *Im
  Explorer anzeigen*, *Nur intern behalten* und *Aus der App entfernen*.

## 11. Versionen, Archiv, Papierkorb und Sicherungen

- **Versionen.** Jede Version einer Notiz wird aufbewahrt; *Versionen…* holt jede davon zurück.
- **Archiv** räumt Dinge aus dem Blick; der **Papierkorb** enthält, was du gelöscht hast, samt Dateien, bis du ihn
  leerst. Beide sind auf Start, mit einem Weg zurück. *Zurücklegen* legt etwas an seinen Platz zurück.
- **Eine Datei einer Notiz im Papierkorb.** Eine aus einer Notiz genommene Datei kommt sofort, ohne Rückfrage, in den
  Papierkorb und verschwindet auf all deinen Geräten und für alle, die die Notiz bekommen, aus der Notiz. *Zurücklegen*
  bringt sie überall zurück. Endgültig weg ist sie erst mit dem Leeren des Papierkorbs.
- **Sicherungen.** *Sicherung exportieren* schreibt eine Datei mit allen Ordnern, Notizen und Dateien. Ohne die Sperre
  (siehe [Privatsphäre, Verschlüsselung und Sicherheit](#14-privatsphäre-verschlüsselung-und-sicherheit)) fragt es nach einem Passwort für sie. *Aus Sicherung
  hinzufügen* holt eine zurück und fragt, ob sie Vorhandenes ergänzt oder ersetzt.

## 12. Der Agent: fragen und erledigen lassen

Der **Agent** (in der oberen Leiste auf dem PC, neben dem Menü auf dem Telefon, oder Strg+K) beantwortet Fragen zu
Mininotes aus der Hilfe der App ([Hilfe auf Englisch](android/app/src/main/resources/org/mininotes/android/help/en.txt),
[auf Französisch](android/app/src/main/resources/org/mininotes/android/help/fr.txt)) und erledigt einfache Aufgaben, auf
Englisch oder Französisch gesagt: *what is new?*, *what is on my todo list?*, *add milk to Grocery*, *share Grocery with
Carol*, *add a contact*, *open*, *find*, *new note called…*, *put … in the dock*. Er läuft auf deinem Gerät, ohne Modell
und Server, sendet nichts, zeigt alles, was andere erreicht oder eine Notiz ändert, bevor er es tut, und bietet an, dich
an die richtige Stelle zu bringen. Ein Tipp außerhalb seines Fensters schließt ihn.

## 13. Auf Minima stempeln mit Integritas

Mit einem [Integritas](https://docs.integritas.technology/)-Konto lässt sich eine Datei oder Notiz über ihr Menü auf der
Minima-Blockchain stempeln. Nur ihr Fingerabdruck (SHA-256) verlässt das Gerät, nie die Datei.

- **Auf Minima stempeln…** hält jetzt fest, dass die Datei genau so existiert, wie sie ist: ein Beweis, dass es sie ab
  diesem Moment gab und sie sich seitdem nicht geändert hat. Es verbraucht einen Stempel deines Integritas-Kontos.
- **Auf Minima prüfen…** prüft eine Datei später gegen das Festgehaltene: ob sie gestempelt wurde, unverändert ist, und
  in welchem Block und an welchem Datum. Es ändert nichts und kostet nichts.
- **Der Unterschied:** Stempeln schreibt den Beweis einmal; Prüfen liest ihn zurück, so oft du willst.
- **Der Stempel einer Notiz** umfasst ihren Titel, eine Leerzeile, dann ihren Text; Umbenennen markiert sie als
  geändert. Stempel vor 0.4.018 umfassen nur den Text: *Notiz, nur Text*.
- **Mein Profil, Stempeln:** Füge deinen API-Schlüssel ein und drück *Speichern*; er liegt in deinem Notizbuch, mit ihm
  gesperrt, und kommt nie in eine Sicherung. Schalte unter *Auch auf meinen Geräten* eigene Geräte ein, und der
  Schlüssel geht auch an sie, für jedes versiegelt.
- **Auf Minima gestempelt…**, ganz unten in *Suchen*, listet, was hier gestempelt wurde; öffne einen Eintrag für
  Fingerabdruck, Block und Transaktion. Sein **+** stempelt eine Notiz oder Datei in Mininotes oder eine Datei dieses
  Geräts außerhalb von Mininotes.

## 14. Privatsphäre, Verschlüsselung und Sicherheit

- **Kein Server, kein Konto, kein Tracking.** Notizen liegen auf deinen Geräten und gehen nur an Personen und Geräte,
  mit denen du sie teilst.
- **Ende zu Ende versiegelt.** Jede Notiz und Datei wird für das Zielgerät versiegelt und dann von
  [Maxima](https://minima.global)-Relays getragen, die Nachrichten weitergeben, die sie nicht lesen können, oder direkt
  von Gerät zu Gerät im selben Netzwerk.
- **Mininotes sperren** (*Einstellungen*, *Sicherheit*) verschlüsselt das Notizbuch, seine Dateien und deine Sicherungen
  auf dem Gerät. Es öffnet sich mit Fingerabdruck oder Displaysperre auf dem Telefon und Windows Hello oder deinem
  Passwort auf dem PC, mit 12 Wiederherstellungswörtern als Ersatzschlüssel: Schreib sie auf. Ohne Sperre sind Notizen
  auf dem Gerät unverschlüsselt.
- **Zwei Dinge nutzen das Netzwerk:** der Maxima-Knoten, der trägt, was du teilst, und ein täglicher, abschaltbarer
  Blick in dieses Repository nach einer neueren Version. [docs/SHARING.md](docs/SHARING.md) sagt, was versiegelt, was
  signiert und was noch nicht gebaut ist.

## 15. Private Bereiche und der Hilferuf

**Private Bereiche**

- **Einen öffnen oder anlegen.** Tippe in irgendeiner Notiz das Wort private, dann //: und dein Passwort, lückenlos am
  Stück, und drück Enter. Der Code verlässt die Notiz, sobald der Doppelpunkt steht, und das Passwort kommt in ein
  eigenes Feld, so bleibt keins davon in der Notiz. Der Bereich dieses Passworts erscheint auf einem eigenen Bildschirm
  über dem ganzen Fenster. Bei einem neuen Passwort tippst du es noch einmal, und ein neuer, leerer Bereich entsteht.
- **Ein Bereich pro Passwort.** Jedes andere Passwort ist ein eigener, getrennter Bereich. Du kannst beliebig viele
  anlegen, und nirgends steht, wie viele du hast.
- **Schreib jedes Passwort auf.** Weil nichts sie festhält, wird ein vertipptes Passwort wie ein neuer Bereich noch
  einmal abgefragt: Meintest du einen bestehenden, schließ das Feld und versuch es erneut. Ein neuer Bereich kann über
  einem früheren liegen und einen Teil davon verlieren.
- **Wo du bist.** Der private Bildschirm hat eine eigene Farbe und ein Schloss neben *Privat*, damit du es immer weißt.
  Lege Notizen, Ordner und Dateien mit dem + unten an. Das Auge beim Passwort zeigt es, solange du es hältst. Zurück
  schließt den Bereich.
- **Einen löschen.** Öffne ihn und wähle *Diesen privaten Bereich löschen* in seinem Menü. Er ist endgültig weg, und
  sein Passwort öffnet danach nichts mehr.
- **Ein langes Passwort.** Nimm einen langen Satz aus mehreren Wörtern. Niemand kann ein vergessenes wiederherstellen,
  auch nicht mit den 12 Wiederherstellungswörtern.
- **Was er fasst.** Ein Bereich fasst etwa 64 MB insgesamt, genug für Notizen und etwa fünfzig Fotos. Seine Notizen,
  Ordner, Fotos und Dateien erscheinen nirgends sonst, bleiben vorerst nur auf diesem Gerät, lassen sich nicht teilen
  oder senden und schließen sich, wenn du Mininotes verlässt oder nach fünf Minuten Ruhe.
- **Ein Code bleibt nie in einer gewöhnlichen Notiz.** Steht einer in einer Notiz, nimmt Mininotes ihn samt Rest seiner
  Zeile heraus, bevor die Notiz gespeichert, gesendet oder geteilt wird.
- **Dateien.** *Von diesem Gerät…* auf dem + fügt dem Bereich eine Datei hinzu, wo sie verschlüsselt liegt. Das Original
  bleibt, wo es war, außer du löschst es, und die Dateiauswahl des Telefons führt eine eigene Liste zuletzt geöffneter
  Dateien.
- **Tipp.** Du kannst einen glaubwürdigen Bereich unter einem Passwort halten und den echten unter einem anderen, und
  den glaubwürdigen ab und zu nutzen, damit er bewohnt wirkt.
- **Ihre Grenzen.** Private Bereiche sind so gebaut, dass niemand, der die Dateien oder den Code der App liest, sie
  sehen oder ihre Zahl erkennen kann. Dass das zu 100 % sicher ist, lässt sich nicht bestätigen, also leg dort nur ab,
  was dir nicht schadet, wenn es jemand findet.

**Notfallkontakt**

- **Was er ist.** Ein Hilferuf sendet deinen Standort und die Worte deiner Wahl an die Personen deiner Wahl, sobald eine
  private Notiz geöffnet wird. Er ist für den Fall, dass du in Gefahr bist und man dich zwingen könnte, Mininotes zu
  öffnen.
- **Einen setzen.** Öffne in einem privaten Bereich eine seiner Notizen, tippe das Wort help, dann //:, alles am Stück,
  und drück Enter. Der Code verschwindet sofort, und ein Fenster fragt, wer benachrichtigt wird und mit welchen Worten.
  Die Notiz, in die du ihn getippt hast, sendet ihn. Nur wer das Passwort des Bereichs kennt, kann einen setzen oder
  entfernen.
- **Nie wieder gezeigt.** Einmal gesetzt, erscheint er nirgends mehr: Kein Menü nennt ihn, und das Fenster öffnet sich
  immer leer.
- **Sie entfernen.** Um alle Hilferufe eines Bereichs zu entfernen, tippe das Wort erase, dann //:, in eine seiner
  Notizen. Auch dieser Code verschwindet sofort.
- **Standort.** Mininotes bittet dafür um die Standortberechtigung des Telefons, und das System zeigt ihre Nutzung an.

## 16. Sprachen und Übersetzung

Mininotes spricht elf Sprachen: Englisch, Französisch, Deutsch, Spanisch, Italienisch, Portugiesisch, Russisch,
Chinesisch, Japanisch, Hindi und Persisch. Beim ersten Start fragt es nach der Sprache, die des Geräts vorgewählt, und
auch dieses README ist in ihr; *Einstellungen, Sprache* ändert beides. *Übersetzen…* im Menü einer Notiz überträgt die
Seite auf dem Gerät in eine andere Sprache, ohne sie an einen Übersetzungsdienst zu senden.

<!-- not in the app -->

## 17. Jeder Menüeintrag in einer Zeile

Auf dem PC sagt der Mauszeiger auf einem Menüeintrag, was er tut. *Mein Profil*, *Textgröße*, *Beschreiben, was jedes
Element tut* schaltet diese Zeilen ein oder aus. Ein Eintrag, der mit „…“ endet, öffnet ein Fenster oder eine Seite;
einer ohne tut es sofort.

| Eintrag | Was er tut |
|---|---|
| Umbenennen | Gib ihm einen anderen Namen. |
| Diesen Tab entfernen | Nimm diesen Tab aus der Notiz. |
| Nach links | Setz diesen Tab eine Stelle weiter nach links. |
| Nach rechts | Setz diesen Tab eine Stelle weiter nach rechts. |
| Bild wählen | Zeig darauf ein eigenes Bild statt seines Symbols. |
| Bild entfernen | Zeig wieder sein Symbol statt des Bildes. |
| Bild | Wähle ein eigenes Bild dafür oder entferne es. |
| Symbol | Wähle das Symbol, das es auf Start und in Listen trägt. |
| Farbe | Färbe es, so kräftig du willst. |
| Schreibfarbe | Die Farbe, in der das Geschriebene dieser Person erscheint. |
| Wird geteilt | Wer es hat und was jeder darf. |
| Teilen | Teile es mit Personen oder deinen Geräten und wähle, was jeder darf. |
| Sync-Einstellungen | Wann und wie seine Änderungen zu den anderen gehen. |
| Jetzt synchronisieren | Sende jetzt, was wartet, und frag die anderen nach ihrem. |
| Meinen Code zeigen | Ein Code, den ein anderes Gerät oder eine Person scannt, um es zu empfangen. |
| Datei über Mininotes teilen | Halte eine Datei dieses PCs mit deinen Geräten synchron. Sie bleibt, wo sie ist. |
| Einen Ordner dieses PCs synchronisieren | Halte einen ganzen Ordner dieses PCs mit deinen Geräten synchron. Seine Dateien bleiben darin. |
| Personen und Geräte | Die Personen und Geräte, mit denen du teilst, und deine Gruppen. |
| Mein Profil | Dein Name, dein Code, deine Verbindung und die Einstellungen der App, die dir gehören. |
| Von einem anderen Gerät | Scanne einen Code oder füge einen Link von einem anderen Gerät oder einer Person ein. |
| Code scannen oder ihren Link einfügen | Füge jemanden oder eines deiner Geräte über seinen Code oder Link hinzu. |
| Meinen Link kopieren | Kopiere deinen Link, um ihn jemandem zu senden, damit er dich hinzufügen kann. |
| Datei anhängen | Bewahre eine Datei dieses PCs bei dieser Notiz auf. |
| Datei hinzufügen | Bewahre eine Datei dieses PCs in diesem Ordner auf. |
| Foto aufnehmen | Öffne die Kamera: Das Foto kommt in diese Notiz. |
| Als Album zeigen | Zeigt seine Bilder in einem Raster, um sie groß durchzublättern. |
| Fotos hinzufügen | Füge mehrere Bilder dieses Geräts auf einmal hinzu. |
| Audio aufnehmen | Nimm Ton in diese Notiz auf. |
| Anhänge | Die Dateien, die bei dieser Notiz liegen. |
| Verschieben nach | Leg es in einen anderen Ordner oder auf Start. |
| Zum Dock hinzufügen | Leg es in die Leiste unten auf Start, einen Klick entfernt. |
| Aus dem Dock entfernen | Nimm es aus dem Dock. Nichts wird gelöscht. |
| Zu Favoriten hinzufügen | Behalte es bei deinen Favoriten und im Dock. |
| Text kopieren | Kopiere den Text der Notiz, um ihn anderswo einzufügen. |
| Übersetzen | Übersetze diese Seite in eine andere Sprache, auf diesem Gerät. |
| Versionen | Jede frühere Version dieser Notiz, zum Ansehen oder Zurücklegen. |
| Auf Minima stempeln | Halte jetzt auf der Minima-Blockchain fest, dass es so existiert, wie es ist. Nur sein Fingerabdruck verlässt dieses Gerät. |
| Auf Minima prüfen | Prüfe, ob es unverändert auf Minima ist und wann es festgehalten wurde. |
| Temporär | Behalte es nur eine Weile: Es geht, wenn seine Zeit um ist. |
| Archiv | Leg es weg, außer Sicht, aufbewahrt. |
| In den Papierkorb | Leg es in den Papierkorb. Es kann zurückgelegt werden, bis der Papierkorb geleert wird. |
| In den Papierkorb verschieben | Leg es in den Papierkorb. Es kann zurückgelegt werden, bis der Papierkorb geleert wird. |
| Neue Notiz | Eine neue Notiz, hier. |
| Neuer Ordner | Ein neuer Ordner, hier. |
| Neuer Ordner in | Ein neuer Ordner in diesem. |
| Alle Seiten | Alle Seiten von Start auf einmal. |
| Zurück zur Hauptseite | Zurück zur ersten Seite von Start. |
| Link öffnen | Öffne diesen Link in deinem Browser. |
| Link kopieren | Kopiere diesen Link. |
| Ausschneiden | Schneide den gewählten Text aus. |
| Kopieren | Kopiere es, um es anderswo einzufügen: ein Bild als Bild, eine Datei als Datei. |
| Einfügen | Füge ein, was kopiert wurde. |
| Alles auswählen | Wähle alles aus. |
| Rückgängig | Nimm die letzte Änderung zurück. |
| Schreiblinien | Zeig oder verbirg die Linien der Seite. |
| Zuletzt | Was du zuletzt geöffnet hast. |
| Temp | Was nur eine Weile bleibt und an deine anderen Geräte geht. |
| Mit mir geteilt | Was andere mit dir geteilt haben und auf deine Antwort wartet. |
| Papierkorb | Was in den Papierkorb gelegt wurde, zum Zurücklegen oder endgültig Löschen. |
| Wiederherstellungsschrift | Geschriebenes, das nicht in seiner Notiz gespeichert werden konnte, beiseitegelegt. |
| Sicherung exportieren | Schreibe eine Datei mit allen Ordnern, Notizen und Dateien. |
| Aus Sicherung hinzufügen | Hol zurück, was eine Sicherungsdatei enthält. |
| Mininotes teilen | Erzähl jemandem von Mininotes. |
| Aktualisieren auf v | Installiere die neuere Version. |
| Feedback | Sag öffentlich etwas über Mininotes, unter deinem Namen. |
| Einstellungen | Wie Mininotes auf diesem Gerät funktioniert. |
| Mininotes auf deinem Handy | Mininotes auf Android-Handys, mit einem Code zum Scannen, um es zu holen. |
| Mininotes auf deinem PC | Mininotes unter Windows, mit dem Link, um es zu holen. |
| Über | Welche Version das ist und wer Mininotes gemacht hat. |
| Mininotes deinstallieren | Entfernt Mininotes von diesem PC, mit oder ohne deine Notizen, nachdem eine Sicherung angeboten wurde. |
| Mininotes beenden | Schließe Mininotes. |
| Dateien senden | Sende Dateien direkt an eines deiner Geräte oder eine Person, ohne Notiz. |
| Gesendete Dateien | Die von hier gesendeten Dateien und wie weit jede ist. |
| Markdown-Datei öffnen | Öffne eine Markdown-Datei dieses PCs als Notiz, die in dieser Datei bleibt. |
| Datei bearbeiten | Öffne eine beliebige Textdatei dieses PCs zum Bearbeiten, ohne sie in Mininotes aufzubewahren. |
| Suchen | Finde Notizen, Ordner und Dateien über ihre Wörter. |
| Agent | Stell eine Frage oder bitte darum, dass etwas getan wird. |
| Baum | Alle Ordner und Notizen auf einmal, an der Seite. |
| Dock | Zeig oder verbirg das Dock auf Start. |
| Menü | Alles, was Mininotes kann. |
| Dinge bleiben | Wie lange bleibt, was in Temp ist. |
| Sprache | Die Sprache der App. |
| Senden, was in Temp ist | Gib, was in Temp ist, an deine anderen Geräte. |
| An wen es geht | Welche deiner Geräte und Personen bekommen, was in Temp ist. |
| Auch auf Start zeigen | Zeig auch auf Start, was in Temp ist. |
| Temp jetzt synchronisieren | Sende jetzt, was in Temp ist. |
| Auf meinem Computer speichern | Speichere eine Kopie an einem Ort deiner Wahl auf diesem PC. |
| Auf meinem Telefon speichern | Speichere eine Kopie an einem Ort deiner Wahl auf diesem Telefon. |
| Auf meinem Gerät speichern | Bewahre es auch als Datei in einem Ordner dieses Geräts auf. |
| Entfernen | Nimm es ab. |
| Öffnen | Öffne es. |
| Öffnen mit | Öffne es in einem anderen Programm. |
| An ein Gerät senden | Sende es direkt an eines deiner Geräte oder eine Person. |
| Von diesem Gerät | Füge Dateien von diesem Gerät hinzu. |
| Welche Version behalten | Es wurde an zwei Stellen geändert: Wähle, welche Version bleibt. |
| Im Explorer anzeigen | Zeig, wo es auf diesem PC liegt. |
| Mit der Datei auf diesem PC synchronisiert | Halte es mit seiner Datei auf diesem PC synchron oder hör damit auf. |
| Durch eine andere Datei ersetzen | Setz eine andere Datei an seine Stelle. |
| In eine Notiz legen | Bewahre es bei einer Notiz auf. |
| Aus Mininotes entfernen | Nimm es aus Mininotes. Die Datei auf diesem PC bleibt. |
| Papierkorb leeren | Lösche alles im Papierkorb endgültig. |
| Auf Start ausblenden | Zeig es nicht auf Start. Es bleibt im Menü. |
| Wartet | Was darauf wartet, zu gehen, und warum. |
| Aus Zuletzt entfernen | Nimm es aus der Liste dessen, was du zuletzt geöffnet hast. |
| Zurücklegen | Leg es zurück, wo es war. |
| Endgültig löschen | Lösche es endgültig. Es kann nicht zurückgelegt werden. |
| Textgröße | Die Größe, in der der Text erscheint. |
| Wie andere Notizen | Zeig diese Notiz in der üblichen Größe. |
| Wiederfinden | Sag Mininotes, wo seine Datei jetzt ist. |
| Nur intern behalten | Bewahre es nicht mehr als Datei auf diesem Gerät auf. Es bleibt in Mininotes. |
| Aus der App entfernen | Nimm es aus Mininotes. Seine Datei auf diesem Gerät bleibt. |
| Gruppe löschen | Lösche diese Gruppe. Die Personen darin bleiben. |
| Mein Gerät | Zähl es zu deinen eigenen Geräten. |
| Vergessen | Nimm dieses Gerät von deiner Liste. Keine Freigabe wird aufgehoben. |
| Diese Person entfernen | Nimm diese Person aus allem, was du mit ihr geteilt hast, und aus deinen Personen. |
| Passwort ändern | Ein neues Passwort für dein Notizbuch. |
| Löschen | Lösche es. |
| Diese Notiz | Was sich mit der offenen Notiz tun lässt. |

<!-- in the app again -->

## 18. Aktualisierungen

Die App sucht täglich nach einer neueren Version und meldet sie in einer Zeile; *Aktualisieren* installiert sie dann.
Die [neueste Version](https://github.com/mininotesorg/mininotes/releases/latest) hat immer die Dateien.

## 19. Sag etwas dazu

*Feedback* im Menü der App öffnet hier ein ausgefülltes Formular unter deinem Namen; nichts wird für dich gepostet.
Alles bisher Gesagte steht in
[den Feedback-Issues](https://github.com/mininotesorg/mininotes/issues?q=is%3Aissue+label%3Afeedback). Ein
**Sicherheitsproblem** geht vertraulich an
[eine Sicherheitsmeldung](https://github.com/mininotesorg/mininotes/security/advisories/new), nicht an ein Issue.
[docs/FEEDBACK.md](docs/FEEDBACK.md) hat den Rest.

<!-- not in the app -->

## 20. Selbst bauen

**Android** braucht JDK 17 und das Android SDK (compileSdk 36):

```sh
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Die APK landet in `android/app/build/outputs/apk/release/`, unsigniert: Signiere sie mit deinem eigenen Schlüssel oder
nutze `./gradlew installDebug`. `android/local.properties` zeigt auf dein SDK und ist nicht im Repository.

**Windows** braucht JDK 17: Führe `./windows/build.ps1 -Package` aus und öffne die `Mininotes.exe` in
`dist/latest/Mininotes-Windows-<version>/`. Siehe [windows/README.md](windows/README.md).

**Prüfungen:** etwa 870 Unit-Tests auf Android und 1.360 auf Windows, ohne Gerät. Sie decken ab, wo ein Fehler jemandes
Geschriebenes kosten würde: das Zusammenführen, ankommende Notizen, wer was darf, die Schema-Migrationen, das Koppeln,
Anhänge. Wofür ein echtes Gerät nötig ist, wird vor jeder Version auf zwei Telefonen und einem PC geprüft, in einem
Protokoll, das der Maintainer privat führt, weil es seine Geräte nennt und aus seinen Notizen zitiert. Frag in einem
Issue nach dem Eintrag für einen Build.

## 21. Was in diesem Repository ist

| Pfad | Was darin ist |
| --- | --- |
| [android/](android/) | Die Android-App und der Code, den beide Apps teilen. Keine XML-Layouts: Die Ansichten werden in Java gebaut. |
| [windows/](windows/README.md) | Die Windows-App und ihre Paketierung. |
| [docs/HOME.md](docs/HOME.md) | Wie die App aufgebaut ist, und jede getroffene Entscheidung, nummeriert. |
| [docs/SHARING.md](docs/SHARING.md) | Das Sync-Design: Schlüssel, Koppeln, Zusammenführen, Konflikte. |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Wofür die App da ist, und wofür nicht. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Wie du helfen kannst und wo du anfängst. |

Wenige Laufzeitabhängigkeiten von Dritten: ZXing für QR-Codes, SQLCipher mit AndroidX SQLite für das verschlüsselte
Notizbuch, FlatLaf für das Aussehen unter Windows, PDFBox für die erste Seite eines PDFs auf dem PC. Es ist so gebaut,
damit sich das Ganze lesen lässt.

<!-- in the app again -->

## 22. Lizenz

Mininotes ist **freie und quelloffene Software** unter der [GNU General Public License, Version 3 oder später](LICENSE).
Du darfst sie nutzen, untersuchen, ändern und weitergeben, auch verkaufen, aber was du weitergibst, geändert oder nicht,
geht mit seinem Quellcode unter derselben Lizenz. Bedingungen Dritter stehen in [NOTICE](NOTICE).

Der Maxima-Transport in der App ist der Kern von [eurobuddha/maxima](https://github.com/eurobuddha/maxima), unverändert
mit Erlaubnis seines Autors übernommen und in NOTICE genannt. Er gehört ihm und steht nicht unter der GPL; sein eigenes
Repository hat noch keine Lizenz.

<!-- not in the app -->

Bis zum 26. September 2026 war Mininotes „source-available“ (Apache 2.0 mit der Commons Clause und einer Bedingung für
bezahlte Produkte); Kopien von davor behalten diese Bedingungen.

Beiträge: [CONTRIBUTING.md](CONTRIBUTING.md). Sicherheitsmeldungen: [SECURITY.md](SECURITY.md).
