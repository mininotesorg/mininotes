<!-- Translated from README.md, SHA-256 3aac5412de459a31bef1e0a93eccaf1c557c9be61015e858fa8724583e1f5000 -->

# Mininotes

Mininotes è un quaderno per il tuo telefono e il tuo PC che mantiene note, cartelle e file uguali su tutti i tuoi
dispositivi e li condivide con le persone che scegli. Non c'è nessun server, nessun account e nessun intermediario:
tutto viaggia sigillato da un capo all'altro sulla rete Minima, o direttamente da dispositivo a dispositivo sulla stessa
Wi-Fi.

[![Ultima versione](https://img.shields.io/github/v/release/mininotesorg/mininotes?label=latest%20build)](https://github.com/mininotesorg/mininotes/releases/latest)

<!-- not in the app -->

[中文](README.zh.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) ·
[Português](README.pt.md) · [English](README.md) · [日本語](README.ja.md) · [Русский](README.ru.md) ·
[हिन्दी](README.hi.md) · [فارسی](README.fa.md)

## Indice

1. [Novità](#1-novità)
2. [Installazione](#2-installazione)
3. [Scrivere](#3-scrivere)
4. [Organizzare: Home, cartelle, dock e preferiti](#4-organizzare-home-cartelle-dock-e-preferiti)
5. [I tuoi dispositivi](#5-i-tuoi-dispositivi)
6. [Condividere con altre persone](#6-condividere-con-altre-persone)
7. [Persone e dispositivi, e Il mio profilo](#7-persone-e-dispositivi-e-il-mio-profilo)
8. [Sapere a che punto è tutto](#8-sapere-a-che-punto-è-tutto)
9. [File, foto e registrazioni](#9-file-foto-e-registrazioni)
10. [I file che restano sul tuo PC](#10-i-file-che-restano-sul-tuo-pc)
11. [Versioni, archivio, cestino e backup](#11-versioni-archivio-cestino-e-backup)
12. [L'Agente: chiedi, e viene fatto](#12-lagente-chiedi-e-viene-fatto)
13. [Marca su Minima con Integritas](#13-marca-su-minima-con-integritas)
14. [Privacy, cifratura e sicurezza](#14-privacy-cifratura-e-sicurezza)
15. [Spazi privati e richiesta di aiuto](#15-spazi-privati-e-richiesta-di-aiuto)
16. [Lingue e traduzione](#16-lingue-e-traduzione)
17. [Ogni voce di menu, in una riga](#17-ogni-voce-di-menu-in-una-riga)
18. [Aggiornamenti](#18-aggiornamenti)
19. [Dicci la tua](#19-dicci-la-tua)
20. [Compilalo da te](#20-compilalo-da-te)
21. [Cosa c'è in questo repository](#21-cosa-cè-in-questo-repository)
22. [Licenza](#22-licenza)

---

## 1. Novità

Le più recenti per prime. Ogni versione è uguale sul telefono e sul PC, salvo dove è indicato diversamente.

### Dalla 0.4.024 alla 0.4.027

- **Marca su Minima…** è una sola riga nel menu di una nota o di un file, che dice a che punto è la sua marcatura, e
  apre una sola finestra con tutto: marcare, cercarla su Minima, verificare di nuovo, le versioni marcate prima e
  confrontare un file con un'impronta. Il rapporto PDF di Integritas si può allegare alla nota, o tenere accanto al
  file.
- Le marcature vanno agli altri tuoi dispositivi: ciò che è marcato sul telefono compare nell'elenco sul PC, e
  viceversa.
- Un file marcato porta un piccolo timbro sulla sua icona.
- *Confronta con un'impronta…*: incolla lo SHA-256 che ti hanno dato per un file e vedi subito se è lo stesso file.
  Niente lascia il dispositivo e non serve alcun account.
- Ciò che risponde Integritas è detto nella tua lingua, e quando non risponde subito gli viene chiesto di nuovo.
- Sul PC, le parole di ogni schermata, pulsante e descrizione sotto il puntatore sono nella tua lingua; anche le prime
  cartelle di un nuovo quaderno prendono il nome in essa. In francese, una nota nuova porta *nouv.*
- Il telefono non smette più di rispondere quando il suo schermo viene ridisegnato mentre sta ancora raggiungendo i
  relay.

### Dalla 0.4.021 alla 0.4.023

- Le schede di una nota si riordinano trascinandole sul PC, o tenendole premute e spostandole sul telefono; il loro menu
  ha anche *Sposta a sinistra* e *Sposta a destra*.
- La lingua viene chiesta a ogni avvio finché non se ne conferma una; sul telefono basta toccare una lingua.
- Ogni app presenta l'altra: *Mininotes sul tuo telefono…* sul PC, *Mininotes sul tuo PC…* sul telefono, con un codice
  da scansionare.
- Il programma di installazione di Windows mostra il logo di Mininotes e può avviare Mininotes quando finisce.
- Una nota iniziata in Temp resta in Temp anche se lo schermo viene ridisegnato prima della sua prima parola.
- I dispositivi non si ripetono più di continuo che hanno un file, cosa che li teneva occupati.

### 0.4.019 e 0.4.020

- Una nuova installazione chiede prima la sua lingua, e il suo README e la sua prima nota sono scritti in essa. README è
  in undici lingue.
- Sul PC, Ctrl+V in Home, in una cartella o in Temp mette lì l'immagine o i file degli appunti.
- Sul PC, un programma di installazione (MSI): nessun diritto di amministratore, Mininotes in Impostazioni, App, e
  *Disinstalla Mininotes…* nel menu, che propone un backup e ti lascia conservare le tue note oppure no. Dopo 30 giorni
  senza backup, Mininotes te lo ricorda.
- Sul PC, le righe con grassetto, corsivo o link non vengono più disegnate una sopra l'altra.
- Sul telefono, ciò che arriva mentre Mininotes è chiuso viene raccolto in una sola notifica.
- *Marcato su Minima…* elenca tutto ciò che è marcato, con un + per marcare qualcos'altro.

### Dalla 0.4.016 alla 0.4.018

- La marcatura di una nota copre il titolo oltre al testo.
- Temp compare in Recenti quando lo usi.
- Il testo incollato da altre app mantiene i suoi a capo.
- Un'immagine messa in Temp raggiunge prima gli altri tuoi dispositivi.

### Dalla 0.4.011 alla 0.4.015

- Sul telefono, Impostazioni, *La tastiera impara dalle mie note*: se attivo, la dettatura vocale della tastiera
  funziona nelle note.
- La chiave Integritas va ai dispositivi che scegli. Il menu *Questa nota* di una nota ha *Marca su Minima…*, un file
  marcato è contrassegnato, e i dettagli di una marcatura rimandano al Minima Explorer.
- Meno lavoro a ogni avvio: i dispositivi non si ripetono più tutto a vicenda.
- Il telefono torna ad ascoltare da solo dopo un riavvio o un aggiornamento.

### 0.4.010

- *Leggimi* ora si chiama **README**: in Home e su GitHub, un unico testo, questo. Un README che nessuno ha modificato
  si riscrive da solo quando una nuova versione ne porta uno nuovo; uno in cui hai scritto resta come l'hai lasciato.

### 0.4.008 e 0.4.009

- Una cartella album **Foto** in Home di serie: le tue immagini in una griglia da sfogliare, ognuna aperta in grande con
  la precedente e la successiva.
- Un risultato di ricerca si apre sulla prima corrispondenza, con una barra di ricerca per passare da una corrispondenza
  all'altra nella nota.
- La conversazione con l'**Agente** usa tutta la finestra, con un pulsante a schermo intero, e si offre di portarti nel
  posto giusto, per esempio la parte Integritas di *Il mio profilo*. Conserva la conversazione per qualche ora.
- *Leggimi* si mostra o si nasconde da *Mostra in Home*, con una sua icona.
- *Leggimi* descrive il contatto di emergenza (la richiesta di aiuto) e i limiti degli spazi privati.

### 0.4.007

- Una nota spostata fuori da una cartella condivisa su uno dei tuoi dispositivi continua a sincronizzarsi con gli altri
  tuoi dispositivi.
- Meno andirivieni tra dispositivi: la stessa risposta non viene più inviata di continuo, così un telefono lavora meno.
- Sul PC, tenendo il puntatore su un tondino accanto al titolo di una nota, vedi chi è, cosa può fare con la nota, a che
  punto è con ciò che vi è scritto e quanti dei suoi file sono ancora in viaggio verso di lui.

### 0.4.006

- La scheda di un PDF mostra la parte alta della prima pagina; quella di un file Word, PowerPoint, Excel, OpenDocument,
  RTF, HTML o di testo mostra le prime righe.
- Sul telefono, *Sceglierne diversi…* su un file tenuto premuto spunta più file da inviare insieme con un'altra app o da
  mettere nel cestino.
- *Il mio profilo* accoglie direttamente la chiave API di Integritas, con *Salva*, e può passarla ai tuoi dispositivi
  che attivi. *Marcato su Minima…* è passato nel menu.
- Sul PC, la dimensione del testo di *Il mio profilo* segue la stessa scala A− e A+ di una nota, e un clic destro in
  Recenti offre *Rimuovi da Recenti*. Il nome dell'app sta a sinistra nella barra, accanto al logo.
- Le parole inviate con il tuo link sono nella lingua che hai scelto.
- Le descrizioni lunghe sotto il puntatore si vedono per intero, su più righe.

### 0.4.005

- Un file di una nota messo nel cestino viene tolto dalla nota su ogni dispositivo e per tutti quelli a cui va la nota.
  *Rimetti a posto* lo riporta ovunque.
- Ciò che attende di partire conta solo i file che la nota mostra, ed elenca ciascuno.
- *Il mio profilo* ha una parte Integritas, con il tuo account e l'elenco di ciò che hai marcato.
- Sul PC, ogni voce di menu ha una breve descrizione che compare sotto il puntatore, attivabile o no in *Il mio
  profilo*.
- Sul telefono, *Copia* su un file tenuto premuto mette negli appunti il file stesso; *Invia con un'altra app…* ha il
  solito simbolo di condivisione.
- Le impostazioni di un gruppo stanno dietro la sua ›; *Il mio profilo* ha *Gestisci…* per Persone e dispositivi; ogni
  voce che apre una finestra o una pagina finisce con "…"; *Tieni come file* ora è *Salva sul mio dispositivo…*.
- La richiesta di aiuto non lascia traccia in nessun menu: si imposta solo con un codice digitato in una nota privata.
- Scrivere sul PC in una nota condivisa molto attiva non rallenta più.

### 0.4.004

- *Persone e dispositivi* e *Il mio profilo* (prima *Profilo*) sono in fondo a *Sincronizzazione e condivisione*.
- Sul PC, *Il mio profilo* imposta la dimensione del testo di tutta l'app, in cinque gradi, dal prossimo avvio.
- *Salva sul mio computer…* (sul telefono *Salva sul mio telefono…*) sostituisce *Scarica* e *Salva una copia…*. Ogni
  finestra che sceglie un file o una cartella sul PC è quella di Windows.
- La richiesta di aiuto è passata solo negli spazi privati.

### 0.4.003

- **Marca su Minima** e **Verifica su Minima** tramite il tuo account Integritas.

### 0.4.002

- Sul PC, *Copia* nel menu di un file mette negli appunti un'immagine come immagine, e qualsiasi file come file.
- Sul telefono, una riga che si apre su una scelta mostra il suo valore a destra, su una riga.

### 0.4.001

- Sul PC, il menu della graffetta spunta i file di una nota per agire su di essi insieme, e ha *Scatta una foto* e
  *Registra audio*.
- *Sincronizza una cartella di questo PC…* è tornato in *Sincronizzazione e condivisione*: una cartella del PC, con i
  suoi file tenuti al suo interno, mostrata in Mininotes con un suo segno.
- Ogni messaggio che un errore può mostrare è tradotto.

## 2. Installazione

**Android** (9 o successivo): scarica `Mininotes-<version>.apk`
dall'[ultima versione](https://github.com/mininotesorg/mininotes/releases/latest), controlla che sia il file pubblicato
e aprilo sul telefono:

```sh
sha256sum -c Mininotes-<version>.apk.sha256
```

Android chiede se consentire l'installazione dal posto da cui l'hai scaricato; quel permesso si può disattivare di nuovo
dopo.

**Controlla chi l'ha firmato.** Ogni APK pubblicato è firmato con la stessa chiave Mininotes. La sua impronta SHA-256
del certificato, nel formato che accetta [AppVerifier](https://github.com/soupslurpr/AppVerifier) (copia le due righe,
poi *Verify from clipboard*):

```
org.mininotes.android
F7:91:C2:14:D9:ED:9B:E9:DE:EC:6F:15:BE:D0:25:38:35:22:DD:F9:A8:21:23:4A:73:90:C8:1B:60:1F:91:AB
```

Oppure con l'SDK di Android: `apksigner verify --print-certs Mininotes-<version>.apk`. Qualsiasi altra impronta non è
una build delle versioni pubblicate da questo repository.

**Windows** (10 o 11): scarica `Mininotes-Windows-<version>.msi` dalla stessa versione e aprilo. Non servono diritti di
amministratore, e Mininotes compare poi in Impostazioni, App di Windows, come qualsiasi app. Disinstallarlo da lì
conserva le tue note, che un'installazione successiva ritrova; *Disinstalla Mininotes…* nel menu dell'app può anche
eliminarle, dopo aver proposto un backup. Java è incluso; non serve installare nient'altro.

Oppure, per chi lo preferisce, `Mininotes-Windows-<version>.zip`: estrailo dove vuoi e apri `Mininotes.exe`.

Su nessuno dei due serve altro: il nodo Maxima che trasporta le tue note è dentro l'app.

<!-- in the app again -->

## 3. Scrivere

- **Una pagina a righe, pronta.** Una nota si apre con il cursore già dentro e si salva mentre scrivi, senza un pulsante
  per salvare.
- **Schede.** Una nota può contenere più pagine, come schede di un indice: *Aggiungi una scheda*. Trascina una scheda
  per spostarla.
- **Grassetto, corsivo e sottolineato.** Scrivi i segni attorno alle parole, `**grassetto**`, `*corsivo*` e
  `__sottolineato__`, oppure premi Ctrl+B, Ctrl+I o Ctrl+U.
- **Due persone in una nota.** Quando due persone scrivono nella stessa nota prima di aver visto il testo dell'altra, i
  due testi vengono uniti riga per riga e non si perde nulla di ciò che hanno scritto. Il testo di ognuno può comparire
  nel suo colore: tocca un tondino sotto il titolo per cambiarne uno.
- **Da altre app.** Su Android, Mininotes è nel menu di condivisione: uno screenshot, foto, file, testo o un link
  finiscono in una nuova nota o in una usata di recente.

## 4. Organizzare: Home, cartelle, dock e preferiti

- **Home** contiene le tue note e cartelle come icone, su quante pagine vuoi, ognuna dove l'hai messa.
- **Crea qualcosa.** Tocca + per creare una nota o una cartella, o per aggiungere file da questo dispositivo o da un
  altro.
- **Il suo menu.** Tieni premuta un'icona per il suo menu: colore, icona, spostamento, condivisione e il resto. Sul PC,
  clic destro; lì il logo riporta in Home, e il divisore si trascina per ridimensionare o chiudere l'elenco laterale.
- **Le cartelle** contengono note e altre cartelle, su tutti i livelli che vuoi. Trascina qualsiasi cosa su una cartella
  per metterla dentro.
- **Spostare.** Tieni premuta e trascina un'icona: dentro una cartella, fuori da una, o su un'altra pagina. Mentre
  trascini, la striscia in alto permette di lasciarla su un preferito, su Temp, sull'archivio, sul cestino o sulla
  condivisione, senza menu.
- **Pagine.** Scorri tra le pagine. Pizzica Home, o scegli *Tutte le pagine* nel menu, per vedere tutte le pagine
  insieme.
- **Colori e icone.** Ogni nota e cartella può avere un suo colore e una sua icona o immagine, dal suo menu. L'intensità
  del colore è sua, regolata con il cursore sotto i colori. Il colore di Home colora tutta l'app.
- **Il dock** è la fila in fondo a Home: ciò che ci metti, a un tocco. *Aggiungi al dock* è nel menu di ogni nota e
  cartella, tranne i preferiti. Ne contiene quanti vuoi e scorre di lato quando non entrano. Ogni dispositivo ha il suo
  dock, mai inviato agli altri tuoi dispositivi.
- **I preferiti** riuniscono ciò a cui torni. Un nuovo preferito va anche nel dock; togliere un preferito lo lascia nel
  dock.
- **Recenti** elenca ciò che hai aperto di recente: sul lato sul PC, in Home sul telefono. *Rimuovi da Recenti* (un clic
  destro sul PC) toglie qualcosa dall'elenco; non viene eliminato nulla. Sul telefono, scorri verso l'alto dal dock, o
  tocca la maniglia appena sopra, per sfogliare Recenti come schede: tocca una scheda per aprirla, scorrila via o premi
  Indietro per chiudere.
- **Temp** è per ciò che ti serve solo per un po', e per passare cose tra i tuoi dispositivi e le persone che scegli: il
  contenuto va a loro e sparisce ovunque alla scadenza.
- **Condivisi con me** contiene ciò che altri hanno condiviso con te, in attesa della tua risposta (vedi
  [Condividere con altre persone](#6-condividere-con-altre-persone)).
- **Cerca**, in fondo a Home, trova note, cartelle e file dalle loro parole. **Albero** mostra tutto insieme.
- **Archivio e Cestino**: fuori dai piedi, o in uscita. Da entrambi si possono rimettere a posto le cose (vedi
  [Versioni, archivio, cestino e backup](#11-versioni-archivio-cestino-e-backup)).
- **README** in Home è questo testo, lo stesso di GitHub, senza ciò che conta solo lì (indice, novità, installazione,
  compilazione ed elenco delle voci di menu, che il PC indica sotto il puntatore).
- **Mostra in Home**, in *Impostazioni*, sceglie quali posti mostra Home (Recenti, Temp, Condivisi con me, l'archivio,
  il cestino) e se c'è il README. Un posto disattivato resta nel menu.
- **Foto**: una cartella album in Home di serie, con le sue immagini in griglia,
  ognuna aperta in grande con la successiva e la precedente a portata di uno scorrimento o di una freccia. Aggiungi foto
  con il suo +, o scattane una. Qualsiasi cartella può essere mostrata come album dal suo menu.

## 5. I tuoi dispositivi

Collega il telefono e il PC una volta sola (*Persone e dispositivi*, poi *Aggiungi il mio dispositivo*, e scansiona con
uno il codice dell'altro). Da allora, ciò che condividi con i tuoi dispositivi si aggiorna da solo su tutti, nei due
sensi, pochi secondi dopo che smetti di scrivere. Un tuo dispositivo può essere impostato su *Può leggere* se deve solo
guardare. Ogni dispositivo ha la sua disposizione: dove metti una nota su uno non la sposta sull'altro, e ognuno ha il
suo dock.

## 6. Condividere con altre persone

- **Aggiungi qualcuno** scansionando il suo codice o aprendo il link che ti invia; lui fa lo stesso con il tuo.
- **Condividi una nota, una cartella o un file** dal suo menu, *Sincronizzazione e condivisione*, poi *Condividi…*.
  Prima scegli cosa potranno fare, poi una persona o un gruppo, o mostra il tuo codice. Ogni persona riceve un ruolo:
  *Proprietario* (chi l'ha creato), *Admin*, *Può scrivere* o *Può leggere*, da cambiare in qualsiasi momento dal menu a
  tendina accanto a lei, o da togliere.
- **Da un altro dispositivo…**, su qualsiasi +, riceve ciò che qualcuno ti mostra.
- **I gruppi**, come Amici o Colleghi, riuniscono persone: condividi con un gruppo e tutti i membri ricevono lo stesso
  ruolo, anche quelli aggiunti dopo.
- **Accetta o rifiuta.** Quando qualcuno condivide con te, una nota te lo dice e l'elemento aspetta in *Condivisi con
  me*. Vedi cos'è e chi l'ha inviato, e scegli dove va, prima che finisca altrove. *Accetta* per tenerlo, o *Rifiuta*,
  in silenzio o con due parole di risposta.
- **I colori viaggiano.** Un colore che dai a una nota condivisa viaggia con lei, e ogni dispositivo può regolarne
  l'intensità.
- **Tutto ciò che è in una cartella condivisa è condiviso.** Qualsiasi cosa metti in una cartella che condividi va alle
  persone con cui l'hai condivisa.
- **I tondini** accanto al titolo di una nota sono le persone e i dispositivi che ce l'hanno, ognuno con la sua immagine
  o la sua iniziale. Un tocco su uno ne mostra l'immagine in grande. Sul PC, tenendo il puntatore su un tondino, vedi
  chi è, cosa può fare con la nota, a che punto è con il testo e quanti dei suoi file sono ancora in viaggio verso di
  lui.

## 7. Persone e dispositivi, e Il mio profilo

Si trovano entrambi in fondo a *Sincronizzazione e condivisione* nel menu.

- **Persone e dispositivi** elenca le persone che hai aggiunto, i tuoi dispositivi e i tuoi gruppi, ognuno su una sua
  pagina. La pagina di una persona mostra ciò che condividi con lei e ti permette di darle un'immagine a tua scelta, che
  vedi solo tu. Le impostazioni di un gruppo stanno dietro la sua ›.
- **Il + accanto a Persone** ti collega a qualcuno: scansiona il suo codice, incolla il suo link, o *Condividi il mio
  link…* per inviare il tuo in un messaggio o in una chat. **Il + accanto a I miei dispositivi** aggiunge un altro dei
  tuoi dispositivi.
- **Il mio profilo** contiene ciò che è tuo:
  - il tuo **nome** e la tua **immagine**, che vanno alle persone con cui sei collegato;
  - il tuo **indirizzo Parlons!**, inviato da solo ai tuoi contatti;
  - il tuo codice e il tuo link, perché gli altri possano aggiungerti;
  - la chiave API di **Integritas** (vedi [Marca su Minima con Integritas](#13-marca-su-minima-con-integritas));
  - sul PC, **Dimensione del testo**: una scala A− e A+ di cinque gradi, da Più piccolo a Il più grande, per tutta l'app
    (Home, Recenti, menu, finestre), applicata dal prossimo avvio, con *Riavvia ora*;
  - sul PC, **Descrivi cosa fa ogni cosa**, attivo di serie, che attiva o disattiva la breve riga mostrata quando il
    puntatore si ferma su una voce di menu o su un tondino;
  - *Gestisci…*, che apre Persone e dispositivi.

## 8. Sapere a che punto è tutto

- **Il segno** su ogni nota, cartella e icona dice a che punto è: un anello grigio vuoto, è solo su questo dispositivo;
  una freccia ambra, aspetta di partire (premila per inviare subito, e vedere ogni nota e file ancora in attesa); tre
  puntini ambra, è inviato e aspetta una risposta; una spunta verde, tutti hanno questa versione; due barre grigie, in
  pausa; un punto esclamativo rosso, un dispositivo non è stato raggiunto da più di tre giorni.
- **Nuovo** elenca ciò che è cambiato dall'ultima volta che hai guardato: note in cui altri hanno scritto, cose
  condivise con te, file arrivati, con chi ha inviato ciascuno, quando e dove si trova.
- **La striscia in alto** compare quando c'è qualcosa che non va con un contatto (un dispositivo non raggiunto da
  giorni, bloccato, o con un Mininotes più vecchio). Mininotes continua a riprovare da solo, e *Cosa fare* ti dice cosa
  puoi fare.

## 9. File, foto e registrazioni

- **Allega** qualsiasi file a una nota o a una cartella, anche più file insieme.
- **Scatta una foto** da una nota: la fotocamera del telefono, o sul PC l'app Fotocamera di Windows, le cui foto
  arrivano direttamente nella nota.
- **Registra audio** in una nota.
- **Poco peso da portare.** Le foto vengono rimpicciolite quando le aggiungi: una nota con cinquanta foto resta leggera;
  si disattiva in *Impostazioni*, *Immagini e file*. Gli altri file vengono compressi per risparmiare spazio.
- **Anteprime.** La scheda di un PDF mostra la prima pagina; quella di un file Word, PowerPoint, Excel, OpenDocument,
  RTF, HTML o di testo mostra le prime righe. Le disegna Mininotes dal file stesso, mai Windows o un'altra app, quindi
  nessuna loro immagine resta fuori.
- **Apri** un file nel programma abituale del computer (*Apri in LibreOffice*, per esempio), o scegline un altro con
  *Apri con…*. Le modifiche che salvi tornano in Mininotes e vanno ai tuoi dispositivi.
- **Copia** su un file (clic destro sul PC, pressione prolungata sul telefono) mette negli appunti il file stesso,
  un'immagine come immagine, da incollare in un'altra app senza salvarlo prima.
- **Salvarlo fuori.** *Salva sul mio computer…* (sul telefono *Salva sul mio telefono…*) salva una copia dove scegli.
  *Salva sul mio dispositivo…* lo tiene anche come file in una cartella di questo dispositivo.
- **Invia con un'altra app…** sul telefono apre la condivisione del telefono, per passare da WhatsApp, per esempio.
  *Sceglierne diversi…* su un file tenuto premuto ne spunta più di uno da inviare così insieme o da mettere nel cestino.
- **Più file insieme sul PC.** Il menu della graffetta ha *Allega un file*, *Scatta una foto* e *Registra audio*, poi i
  file della nota, ognuno con una casella da spuntare: *Seleziona tutto*, poi *Salva sul mio computer…* (più file vanno
  in una sola cartella), *Marca su Minima…* o *Sposta nel cestino* per quelli spuntati.
- **Invia file** direttamente a un altro tuo dispositivo o a qualcuno che hai aggiunto, senza nessuna nota, da porta a
  porta sulla stessa Wi-Fi. Ciò che arriva finisce in Home, segnato come *nuovo* finché non lo apri.
- **Marca su Minima…** (nel menu di un file o di una nota): vedi
  [Marca su Minima con Integritas](#13-marca-su-minima-con-integritas).

## 10. I file che restano sul tuo PC

- **Condividi un file tramite Mininotes** (sul PC, in *Sincronizzazione e condivisione*): un foglio di calcolo o un
  documento resta dov'è sul tuo PC e si apre nel suo programma. Ogni volta che lo salvi, quella versione va agli altri
  tuoi dispositivi, e una versione salvata altrove viene riscritta al suo interno. Viaggia il file intero, nell'ultima
  versione: le modifiche dentro un foglio di calcolo non vengono unite. Se è cambiato in due posti prima che si
  incontrassero, Mininotes chiede quale versione tenere e non sostituisce nulla finché non rispondi.
- **Sincronizza una cartella di questo PC** (sul PC, in *Sincronizzazione e condivisione*): scegli una cartella, su C: o
  dove vuoi, e tutto ciò che contiene compare in Mininotes come una cartella segnata come residente sul tuo PC. I file
  che ci metti, da Esplora file o da qualsiasi programma, vengono presi pochi secondi dopo e vanno ai tuoi dispositivi e
  a chiunque con cui condividi la cartella; i file che arrivano da loro vengono scritti al suo interno. I file restano
  in quella cartella, fuori dall'app. Eliminarne uno sul disco non lo elimina mai in Mininotes, né il contrario; se un
  file è cambiato in entrambi i posti, Mininotes chiede quale versione tenere. Su un telefono i file della cartella sono
  tenuti dentro l'app.
- **Le note come file.** Una nota o una cartella può anche essere tenuta come file Markdown in una cartella a tua scelta
  sul PC (*Salva sul mio dispositivo…*). Mininotes non elimina mai quei file, e una nota privata non viene mai scritta
  fuori.
- Una cartella così ha sul suo riquadro una cartella con le frecce. Il suo menu dice dove si trova sul PC, poi *Mostra
  in Esplora file*, *Tieni solo nell'app* e *Rimuovi dall'app*.

## 11. Versioni, archivio, cestino e backup

- **Versioni.** Ogni versione di una nota viene conservata; *Versioni…* rimette a posto quella che vuoi.
- **Archivio** mette via le cose, fuori dalla vista; il **cestino** tiene ciò che hai eliminato, file compresi, finché
  non lo svuoti. Entrambi sono in Home, con una via per uscirne. *Rimetti a posto* rimette una cosa dov'era.
- **Il file di una nota nel cestino.** Un file tolto da una nota va subito nel cestino, senza domande, e viene tolto
  dalla nota su tutti i tuoi dispositivi e per tutti quelli a cui va la nota. *Rimetti a posto* lo riporta ovunque. Se
  ne va per sempre solo quando il cestino viene svuotato.
- **Backup.** *Esporta backup* scrive un unico file con tutte le cartelle, note e file. Senza il blocco (vedi
  [Privacy, cifratura e sicurezza](#14-privacy-cifratura-e-sicurezza)), chiede una password per quel backup. *Aggiungi
  da backup* ne recupera uno e chiede se aggiungere a ciò che c'è o sostituirlo.

## 12. L'Agente: chiedi, e viene fatto

L'**Agente** (nella barra in alto sul PC, accanto al menu sul telefono, o Ctrl+K) risponde alle domande su Mininotes
grazie alla guida contenuta nell'app
([guida in inglese](android/app/src/main/resources/org/mininotes/android/help/en.txt),
[en français](android/app/src/main/resources/org/mininotes/android/help/fr.txt)), e svolge compiti semplici chiesti in
inglese o in francese: *what is new?*, *what is on my todo list?*, *add milk to Grocery*, *share Grocery with Carol*,
*add a contact*, *open*, *find*, *new note called…*, *put … in the dock*. Funziona sul tuo dispositivo senza modelli né
server, non invia nulla, ti mostra tutto ciò che raggiunge altre persone o modifica una nota prima di farlo, e si offre
di portarti nel posto giusto. Un tocco fuori dalla sua finestra lo chiude.

## 13. Marca su Minima con Integritas

Con un account [Integritas](https://docs.integritas.technology/), un file o una nota può essere marcato sulla blockchain
di Minima dal suo menu. Lascia il dispositivo solo la sua impronta (SHA-256), mai il file.

- **Marca su Minima…** registra ora che il file esiste esattamente così com'è: una prova, da quel momento, che esisteva
  e che da allora non è cambiato. Usa una marcatura del tuo account Integritas.
- **Verifica su Minima…** controlla un file, in seguito, rispetto a ciò che è stato registrato: se è stato marcato, se è
  invariato, e in quale blocco e in che data. Non cambia nulla e non costa nulla.
- **La differenza:** marcare scrive la prova una volta; verificare la rilegge, tutte le volte che vuoi.
- **La marcatura di una nota** copre il titolo, una riga vuota e poi il testo; rinominarla la segna come cambiata. Le
  marcature fatte prima della 0.4.018 coprono solo il testo: *Nota, solo testo*.
- **Il mio profilo, Marcatura:** incolla la tua chiave API e premi *Salva*; viene tenuta nel tuo quaderno, bloccata con
  lui, e mai messa in un backup. Attiva i tuoi dispositivi sotto *Anche sui miei dispositivi* e la chiave va anche a
  loro, sigillata per ciascuno.
- **Marcato su Minima…**, ultimo in *Cerca*, elenca ciò che è stato marcato qui; aprine uno per vederne impronta,
  blocco e transazione. Il suo **+** marca una nota o un file in Mininotes, o un file di questo dispositivo che non è
  stato messo in Mininotes.

## 14. Privacy, cifratura e sicurezza

- **Nessun server, nessun account, nessun tracciamento.** Le note restano sui tuoi dispositivi e vanno solo a persone e
  dispositivi con cui le condividi.
- **Sigillato da un capo all'altro.** Ogni nota e ogni file è sigillato per il dispositivo a cui va, poi trasportato dai
  relay [Maxima](https://minima.global), che passano messaggi che non possono leggere, o direttamente da dispositivo a
  dispositivo sulla stessa rete.
- **Blocca Mininotes** (*Impostazioni*, *Sicurezza*) cifra il quaderno, i suoi file e i tuoi backup sul dispositivo. Si
  apre con l'impronta digitale o il blocco schermo sul telefono e con Windows Hello o la tua password sul PC, con 12
  parole di recupero come chiave di riserva: scrivile da qualche parte. Senza blocco, le note non sono cifrate sul
  dispositivo.
- **Due cose usano la rete:** il nodo Maxima che trasporta ciò che condividi, e un controllo al giorno su questo
  repository per una versione più recente, che puoi disattivare. [docs/SHARING.md](docs/SHARING.md) spiega cosa è
  sigillato, cosa è firmato e cosa non è ancora stato realizzato.

## 15. Spazi privati e richiesta di aiuto

**Spazi privati**

- **Aprirne o crearne uno.** In qualsiasi nota, scrivi la parola private, poi //: e la tua password, tutto attaccato
  senza niente in mezzo, e premi Invio. Il codice lascia la nota appena scrivi i due punti, e la password va in una
  finestra a parte, così nessuno dei due resta nella nota. Lo spazio si apre su una schermata a parte, sopra tutta la
  finestra. Con una password mai usata prima, scrivila di nuovo e viene creato un nuovo spazio vuoto.
- **Uno spazio per ogni password.** Ogni password diversa è uno spazio separato. Non c'è limite a quanti puoi crearne, e
  da nessuna parte resta traccia di quanti ne hai.
- **Scrivi ogni password da qualche parte.** Siccome niente le registra, una password digitata male chiede di essere
  ripetuta come per un nuovo spazio: se succede quando volevi uno spazio esistente, chiudi la finestra e riprova. Un
  nuovo spazio può sovrapporsi a uno precedente e farne perdere una parte.
- **Dove sei.** La schermata privata ha un suo colore e un lucchetto accanto a *Privato*, così sai sempre dove sei. Crea
  note, cartelle e file con il + in basso. L'occhio accanto alla password la mostra finché lo tieni premuto. Indietro
  chiude lo spazio.
- **Eliminarne uno.** Aprilo e scegli *Elimina questo spazio privato…* nel suo menu. Se ne va per sempre, e la sua
  password poi non apre più nulla.
- **Una password lunga.** Usa come password una frase lunga di più parole. Nessuno può recuperarne una dimenticata,
  neppure con le 12 parole di recupero.
- **Cosa contiene.** Circa 64 MB in tutto, abbastanza per le note e una cinquantina di foto. Le sue note, cartelle, foto
  e file non compaiono da nessun'altra parte, per ora restano solo su questo dispositivo, non si possono condividere né
  inviare, e si chiudono quando esci da Mininotes o dopo cinque minuti senza uso.
- **Un codice non resta mai in una nota normale.** Se ne rimane uno in una nota, Mininotes lo toglie, con il resto della
  sua riga, prima che la nota venga salvata, inviata o condivisa.
- **File.** *Da questo dispositivo…* sul + aggiunge un file allo spazio, dove viene tenuto cifrato. L'originale resta
  dov'era a meno che tu non scelga di eliminarlo, e il selettore di file del telefono tiene un suo elenco dei file
  aperti di recente.
- **Un consiglio.** Puoi tenere uno spazio credibile sotto una password e quello vero sotto un'altra, e usare quello
  credibile ogni tanto perché sembri vissuto.
- **I loro limiti.** Gli spazi privati sono fatti in modo che nessuno, leggendo i file dell'app o il suo codice, possa
  vederli o capire quanti sono. Non si può confermare che siano sicuri al 100%, quindi tienici solo cose che non ti
  danneggerebbero se venissero trovate.

**Contatto di emergenza**

- **Cos'è.** Una richiesta di aiuto invia la tua posizione, e le parole che hai scelto, alle persone che hai scelto, nel
  momento in cui viene aperta una nota privata. Serve quando potresti essere in pericolo e qualcuno potrebbe
  costringerti ad aprire Mininotes.
- **Impostarne una.** In una nota di uno spazio privato, scrivi la parola help, poi //:, tutto attaccato, e premi Invio.
  Il codice sparisce subito e una finestra chiede chi avvisare e cosa dire. La nota in cui l'hai scritto è quella che la
  invia. Solo chi conosce la password dello spazio può impostarne una o toglierla.
- **Mai più mostrata.** Una volta impostata non viene più mostrata, da nessuna parte: nessun menu la nomina, e la
  finestra si apre sempre vuota.
- **Toglierle.** Per togliere tutte le richieste di uno spazio, scrivi la parola erase, poi //:, in una delle sue note.
  Anche quel codice sparisce subito.
- **Posizione.** Per questo Mininotes chiede il permesso di localizzazione del telefono, e il sistema mostra quando
  viene usato.

## 16. Lingue e traduzione

Mininotes parla undici lingue: inglese, francese, tedesco, spagnolo, italiano, portoghese, russo, cinese, giapponese,
hindi e persiano. Al primo avvio chiede quale, con quella del dispositivo già scelta, e anche questo README è in quella
lingua; *Impostazioni, Lingua* cambia entrambi. *Traduci…* nel menu di una nota porta la pagina in un'altra lingua sul
dispositivo, senza inviarla a un servizio di traduzione.

<!-- not in the app -->

## 17. Ogni voce di menu, in una riga

Sul PC, tenendo il puntatore su una voce di menu, vedi cosa fa. *Il mio profilo*, *Dimensione del testo*, *Descrivi cosa
fa ogni cosa* attiva o disattiva queste righe. Una voce che finisce con "…" apre una finestra o una pagina; una senza lo
fa subito.

| Voce | Cosa fa |
|---|---|
| Rinomina | Dagli un altro nome. |
| Rimuovi questa scheda | Togli questa scheda dalla nota. |
| Sposta a sinistra | Metti questa scheda un posto più a sinistra. |
| Sposta a destra | Metti questa scheda un posto più a destra. |
| Scegli un'immagine | Mostra una tua immagine al posto della sua icona. |
| Rimuovi l'immagine | Mostra di nuovo la sua icona al posto dell'immagine. |
| Immagine | Scegli o rimuovi una tua immagine per esso. |
| Icona | Scegli l'icona che mostra in Home e negli elenchi. |
| Colore | Coloralo, con l'intensità che scegli. |
| Colore di scrittura | Il colore in cui appare ciò che scrive questa persona. |
| Condivisione | Chi lo ha e cosa può fare ciascuno. |
| Condividi | Condividilo con persone o con i tuoi dispositivi, e scegli cosa può fare ciascuno. |
| Parametri di sincronizzazione | Quando e come le sue modifiche vanno agli altri. |
| Sincronizza ora | Invia ora ciò che è in attesa, e chiedi agli altri il loro. |
| Mostra il mio codice | Un codice da far scansionare a un altro dispositivo o a una persona, per riceverlo. |
| Condividi un file tramite Mininotes | Sincronizza un file di questo PC con i tuoi dispositivi. Resta dov'è. |
| Sincronizza una cartella di questo PC | Sincronizza un'intera cartella di questo PC con i tuoi dispositivi. I suoi file restano lì. |
| Persone e dispositivi | Le persone e i dispositivi con cui condividi, e i tuoi gruppi. |
| Il mio profilo | Il tuo nome, il tuo codice, la tua connessione e le impostazioni dell'app che sono tue. |
| Da un altro dispositivo | Scansiona un codice o incolla un link da un altro dispositivo o da una persona. |
| Scansiona un codice o incolla il loro link | Aggiungi qualcuno, o uno dei tuoi dispositivi, dal suo codice o link. |
| Copia il mio link | Copia il tuo link, da inviare a qualcuno perché possa aggiungerti. |
| Allega un file | Tieni un file di questo PC con questa nota. |
| Aggiungi un file | Tieni un file di questo PC in questa cartella. |
| Scatta una foto | Apri la fotocamera: la foto entra in questa nota. |
| Mostra come album | Mostra le sue immagini in una griglia, per sfogliarle in grande. |
| Aggiungi foto | Aggiungi più immagini di questo dispositivo in una volta. |
| Registra audio | Registra un audio in questa nota. |
| Allegati | I file tenuti con questa nota. |
| Sposta in | Mettilo in un'altra cartella, o in Home. |
| Aggiungi al dock | Mettilo nella fila in fondo a Home, a un clic. |
| Rimuovi dal dock | Toglilo dal dock. Non viene eliminato nulla. |
| Aggiungi ai preferiti | Tienilo tra i preferiti, e nel dock. |
| Copia il testo | Copia il testo della nota, per incollarlo altrove. |
| Traduci | Traduci questa pagina in un'altra lingua, su questo dispositivo. |
| Versioni | Tutte le versioni precedenti di questa nota, da guardare o rimettere a posto. |
| Marca su Minima | Registra ora sulla blockchain Minima che esiste così com'è. Solo la sua impronta lascia questo dispositivo. |
| Verifica su Minima | Controlla se è su Minima invariato, e quando è stato registrato. |
| Temporaneo | Tienilo solo per un po': se ne va quando il suo tempo è scaduto. |
| Archivio | Mettilo da parte, fuori vista, conservato. |
| Sposta nel cestino | Mettilo nel cestino. Si può rimettere a posto finché il cestino non viene svuotato. |
| Sposta nel cestino | Mettilo nel cestino. Si può rimettere a posto finché il cestino non viene svuotato. |
| Nuova nota | Una nuova nota, qui. |
| Nuova cartella | Una nuova cartella, qui. |
| Nuova cartella in | Una nuova cartella dentro questa. |
| Tutte le pagine | Tutte le pagine di Home insieme. |
| Torna alla pagina principale | Torna alla prima pagina di Home. |
| Apri il link | Apri questo link nel tuo browser. |
| Copia il link | Copia questo link. |
| Taglia | Taglia il testo scelto. |
| Copia | Copialo, per incollarlo altrove: un'immagine come immagine, un file come file. |
| Incolla | Incolla ciò che è stato copiato. |
| Seleziona tutto | Seleziona tutto. |
| Annulla | Annulla l'ultima modifica. |
| Righe di scrittura | Mostra o nascondi le righe della pagina. |
| Recenti | Ciò che hai aperto di recente. |
| Temp | Ciò che è tenuto solo per un po', e va ai tuoi altri dispositivi. |
| Condivisi con me | Ciò che altri hanno condiviso con te, in attesa della tua risposta. |
| Cestino | Ciò che è stato messo nel cestino, da rimettere a posto o eliminare per sempre. |
| Scrittura di recupero | Testo che non è stato possibile salvare nella sua nota, messo da parte. |
| Esporta backup | Scrivi un unico file con tutte le cartelle, note e file. |
| Aggiungi da backup | Recupera ciò che contiene un file di backup. |
| Condividi Mininotes | Parla di Mininotes a qualcuno. |
| Aggiorna alla v | Installa la versione più recente. |
| Feedback | Di' qualcosa su Mininotes, pubblicamente, con il tuo nome. |
| Impostazioni | Come funziona Mininotes su questo dispositivo. |
| Mininotes sul tuo telefono | Mininotes sui telefoni Android, con un codice da scansionare per averlo. |
| Mininotes sul tuo PC | Mininotes su Windows, con il link per averlo. |
| Informazioni | Quale versione è questa, e chi ha fatto Mininotes. |
| Disinstalla Mininotes | Rimuove Mininotes da questo PC, tenendo o no le tue note, dopo aver proposto un backup. |
| Esci da Mininotes | Chiudi Mininotes. |
| Invia file | Invia file direttamente a uno dei tuoi dispositivi o a una persona, senza nessuna nota. |
| File inviati | I file inviati da qui, e a che punto è ciascuno. |
| Apri un file Markdown | Apri un file Markdown di questo PC come una nota tenuta in quel file. |
| Modifica un file | Apri qualsiasi file di testo di questo PC per modificarlo, senza tenerlo in Mininotes. |
| Cerca | Trova note, cartelle e file dalle loro parole. |
| Agente | Fai una domanda, o chiedi che venga fatto qualcosa. |
| Albero | Tutte le cartelle e le note insieme, sul lato. |
| Dock | Mostra o nascondi il dock in Home. |
| Menu | Tutto ciò che Mininotes sa fare. |
| Gli elementi restano | Per quanto tempo resta ciò che è in Temp. |
| Lingua | La lingua dell'app. |
| Invia ciò che è in Temp | Dai ciò che è in Temp ai tuoi altri dispositivi. |
| A chi va | Quali tuoi dispositivi e persone ricevono ciò che è in Temp. |
| Mostra anche in Home | Mostra anche in Home ciò che è in Temp. |
| Sincronizza Temp ora | Invia ora ciò che è in Temp. |
| Salva sul mio computer | Salva una copia dove scegli su questo PC. |
| Salva sul mio telefono | Salva una copia dove scegli su questo telefono. |
| Salva sul mio dispositivo | Tienilo anche come file in una cartella di questo dispositivo. |
| Rimuovi | Toglilo. |
| Apri | Aprilo. |
| Apri con | Aprilo in un altro programma. |
| Invia a un dispositivo | Invialo direttamente a uno dei tuoi dispositivi o a una persona. |
| Da questo dispositivo | Aggiungi file da questo dispositivo. |
| Quale versione tenere | È cambiato in due posti: scegli quale versione resta. |
| Mostra in Esplora file | Mostra dov'è su questo PC. |
| Sincronizzato con il file di questo PC | Sincronizzalo con il suo file su questo PC, o smetti. |
| Sostituisci con un altro file | Metti un altro file al suo posto. |
| Metti in una nota | Tienilo con una nota. |
| Rimuovi da Mininotes | Toglilo da Mininotes. Il file su questo PC resta. |
| Svuota il cestino | Elimina per sempre tutto ciò che è nel cestino. |
| Nascondi da Home | Non mostrarlo in Home. Resta nel menu. |
| In attesa | Ciò che attende di partire, e perché. |
| Rimuovi da Recenti | Toglilo dall'elenco di ciò che hai aperto di recente. |
| Rimetti a posto | Rimettilo dov'era. |
| Elimina per sempre | Eliminalo per sempre. Non si potrà rimettere a posto. |
| Dimensione del testo | La dimensione con cui appare il testo. |
| Come le altre note | Mostra questa nota alla dimensione abituale. |
| Ritrovalo | Di' a Mininotes dov'è ora il suo file. |
| Tieni solo nell'app | Smetti di tenerlo come file su questo dispositivo. Resta in Mininotes. |
| Rimuovi dall'app | Toglilo da Mininotes. Il suo file su questo dispositivo resta. |
| Elimina il gruppo | Elimina questo gruppo. Le persone che ne fanno parte restano. |
| Il mio dispositivo | Consideralo uno dei tuoi dispositivi. |
| Dimentica | Togli questo dispositivo dal tuo elenco. Nessuna condivisione viene tolta. |
| Rimuovi questa persona | Togli questa persona da tutto ciò che hai condiviso con lei, e dalle tue persone. |
| Cambia password | Una nuova password per il tuo quaderno. |
| Elimina | Eliminalo. |
| Questa nota | Cosa si può fare con la nota aperta. |

<!-- in the app again -->

## 18. Aggiornamenti

L'app controlla una volta al giorno se c'è una versione più recente e lo dice in una riga; *Aggiorna* poi la installa.
L'[ultima versione](https://github.com/mininotesorg/mininotes/releases/latest) ha sempre i file.

## 19. Dicci la tua

*Feedback* nel menu dell'app apre qui un modulo già compilato, con il tuo nome; niente viene pubblicato al posto tuo.
Tutto ciò che chiunque ha detto è nelle
[issue di feedback](https://github.com/mininotesorg/mininotes/issues?q=is%3Aissue+label%3Afeedback). Un problema di
**sicurezza** va segnalato in privato con
[un avviso di sicurezza](https://github.com/mininotesorg/mininotes/security/advisories/new), non in una issue.
[docs/FEEDBACK.md](docs/FEEDBACK.md) dice il resto.

<!-- not in the app -->

## 20. Compilalo da te

**Android** richiede il JDK 17 e l'SDK di Android (compileSdk 36):

```sh
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

L'APK finisce in `android/app/build/outputs/apk/release/`, non firmato: firmalo con la tua chiave, o usa `./gradlew
installDebug`. `android/local.properties` punta al tuo SDK e non è nel repository.

**Windows** richiede il JDK 17: esegui `./windows/build.ps1 -Package` e apri il `Mininotes.exe` in
`dist/latest/Mininotes-Windows-<version>/`. Vedi [windows/README.md](windows/README.md).

**Controlli:** circa 870 test unitari su Android e 1.360 su Windows, senza bisogno di un dispositivo. Coprono i punti in
cui un errore farebbe perdere ciò che qualcuno ha scritto: l'unione, le note in arrivo, chi può fare cosa, le migrazioni
dello schema, l'abbinamento, gli allegati. Ciò per cui serve un dispositivo vero viene controllato su due telefoni e un
PC prima di ogni versione, in un registro che il manutentore tiene privato, perché nomina i suoi dispositivi e cita le
sue note. Chiedi in una issue la voce relativa a una build.

## 21. Cosa c'è in questo repository

| Percorso | Cosa contiene |
| --- | --- |
| [android/](android/) | L'app Android, e il codice che le due app condividono. Niente layout XML: le viste sono costruite in Java. |
| [windows/](windows/README.md) | L'app Windows e il suo pacchetto. |
| [docs/HOME.md](docs/HOME.md) | Com'è organizzata l'app, e ogni decisione presa, numerata. |
| [docs/SHARING.md](docs/SHARING.md) | Il progetto della sincronizzazione: chiavi, abbinamento, unione, conflitti. |
| [docs/PRODUCT.md](docs/PRODUCT.md) | A cosa serve l'app, e cosa non è. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Come aiutare, e da dove cominciare. |

Poche dipendenze di terze parti in esecuzione: ZXing per i codici QR, SQLCipher con AndroidX SQLite per il quaderno
cifrato, FlatLaf per l'aspetto su Windows, PDFBox per la prima pagina di un PDF sul PC. È fatto così perché si possa
leggere tutto.

<!-- in the app again -->

## 22. Licenza

Mininotes è **software libero e open source** sotto la [GNU General Public License, versione 3 o successiva](LICENSE).
Puoi usarlo, studiarlo, modificarlo e passarlo ad altri, e anche venderlo, ma qualsiasi cosa passi ad altri, modificata
o no, va accompagnata dal suo codice sorgente sotto la stessa licenza. Le condizioni di terze parti sono in
[NOTICE](NOTICE).

Il trasporto Maxima dentro l'app è il nucleo di [eurobuddha/maxima](https://github.com/eurobuddha/maxima), incluso senza
modifiche con il permesso del suo autore e citato in NOTICE. È suo, e non è sotto la GPL; il suo repository non ha
ancora una licenza.

<!-- not in the app -->

Fino al 26 settembre 2026 Mininotes era a sorgente disponibile (Apache 2.0 con la Commons Clause e una condizione sui
prodotti a pagamento); le copie prese prima di allora mantengono quei termini.

Contributi: [CONTRIBUTING.md](CONTRIBUTING.md). Segnalazioni di sicurezza: [SECURITY.md](SECURITY.md).
