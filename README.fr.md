<!-- Translated from README.md, SHA-256 3aac5412de459a31bef1e0a93eccaf1c557c9be61015e858fa8724583e1f5000 -->

# Mininotes

Mininotes est un carnet pour votre téléphone et votre PC qui garde vos notes, dossiers et fichiers identiques sur tous
vos appareils et les partage avec les personnes de votre choix. Il n'y a ni serveur, ni compte, ni intermédiaire : tout
voyage scellé de bout en bout sur le réseau Minima, ou directement d'un appareil à l'autre sur le même Wi-Fi.

[![Dernière version](https://img.shields.io/github/v/release/mininotesorg/mininotes?label=latest%20build)](https://github.com/mininotesorg/mininotes/releases/latest)

<!-- not in the app -->

[中文](README.zh.md) · [Español](README.es.md) · [English](README.md) · [Deutsch](README.de.md) ·
[Português](README.pt.md) · [Italiano](README.it.md) · [日本語](README.ja.md) · [Русский](README.ru.md) ·
[हिन्दी](README.hi.md) · [فارسی](README.fa.md)

## Sommaire

1. [Nouveautés](#1-nouveautés)
2. [Installation](#2-installation)
3. [Écrire](#3-écrire)
4. [Organiser : Accueil, dossiers, dock et favoris](#4-organiser--accueil-dossiers-dock-et-favoris)
5. [Vos propres appareils](#5-vos-propres-appareils)
6. [Partager avec d'autres personnes](#6-partager-avec-dautres-personnes)
7. [Personnes et appareils, et Mon profil](#7-personnes-et-appareils-et-mon-profil)
8. [Savoir où en est chaque chose](#8-savoir-où-en-est-chaque-chose)
9. [Fichiers, photos et enregistrements](#9-fichiers-photos-et-enregistrements)
10. [Les fichiers qui restent sur votre PC](#10-les-fichiers-qui-restent-sur-votre-pc)
11. [Versions, archives, corbeille et sauvegardes](#11-versions-archives-corbeille-et-sauvegardes)
12. [L'Agent : demandez, et c'est fait](#12-lagent--demandez-et-cest-fait)
13. [Horodater sur Minima avec Integritas](#13-horodater-sur-minima-avec-integritas)
14. [Confidentialité, chiffrement et sécurité](#14-confidentialité-chiffrement-et-sécurité)
15. [Espaces privés et demande d'aide](#15-espaces-privés-et-demande-daide)
16. [Langues et traduction](#16-langues-et-traduction)
17. [Chaque entrée de menu, en une ligne](#17-chaque-entrée-de-menu-en-une-ligne)
18. [Mises à jour](#18-mises-à-jour)
19. [Donnez votre avis](#19-donnez-votre-avis)
20. [Le compiler vous-même](#20-le-compiler-vous-même)
21. [Le contenu de ce dépôt](#21-le-contenu-de-ce-dépôt)
22. [Licence](#22-licence)

---

## 1. Nouveautés

Les plus récentes d'abord. Chaque version est la même sur le téléphone et sur le PC, sauf mention contraire.

### 0.4.024 à 0.4.027

- **Horodater sur Minima…** est une seule ligne dans le menu d'une note ou d'un fichier, qui dit où en est son
  horodatage, et ouvre une seule fenêtre avec tout : horodater, le chercher sur Minima, vérifier de nouveau, les
  versions horodatées avant, et comparer un fichier avec une empreinte. Le rapport PDF d'Integritas peut être attaché à
  la note, ou gardé à côté du fichier.
- Les horodatages vont à vos autres appareils : ce qui est horodaté sur le téléphone est listé sur le PC, et
  inversement.
- Un fichier horodaté porte un petit tampon sur son icône.
- *Comparer avec une empreinte…* : collez le SHA-256 qu'on vous a donné pour un fichier et voyez aussitôt si c'est le
  même fichier. Rien ne quitte l'appareil et aucun compte n'est nécessaire.
- Ce que répond Integritas est dit dans votre langue, et quand il ne répond pas tout de suite, la question lui est
  reposée.
- Sur le PC, les mots de chaque écran, bouton et description sous le pointeur sont dans votre langue ; les premiers
  dossiers d'un nouveau carnet y sont nommés aussi. En français, une note nouvelle porte *nouv.*
- Le téléphone ne cesse plus de répondre quand son écran est redessiné alors qu'il cherche encore à joindre les relais.

### 0.4.021 à 0.4.023

- Les onglets d'une note changent d'ordre en les faisant glisser sur le PC, ou en les maintenant puis en les déplaçant
  sur le téléphone ; leur menu a aussi *Déplacer à gauche* et *Déplacer à droite*.
- La langue est demandée à chaque démarrage jusqu'à ce qu'une langue soit confirmée ; sur le téléphone, toucher une
  langue suffit.
- Chaque app présente l'autre : *Mininotes sur votre téléphone…* sur le PC, *Mininotes sur votre PC…* sur le téléphone,
  avec un code à scanner.
- Le programme d'installation Windows montre le logo de Mininotes et peut lancer Mininotes quand il a fini.
- Une note commencée dans Temp reste dans Temp même si l'écran est redessiné avant son premier mot.
- Les appareils ne se répètent plus sans cesse qu'ils ont un fichier, ce qui les occupait.

### 0.4.019 à 0.4.020

- Une nouvelle installation demande d'abord sa langue, et son README et sa première note sont écrits dans cette langue.
  Le README existe en onze langues.
- Sur le PC, Ctrl+V sur l'accueil, dans un dossier ou dans Temp y dépose l'image ou les fichiers du presse-papiers.
- Sur le PC, un programme d'installation (MSI) : aucun droit d'administrateur, Mininotes dans Paramètres, Applications,
  et *Désinstaller Mininotes…* dans le menu, qui propose une sauvegarde et vous laisse garder vos notes ou non. Après 30
  jours sans sauvegarde, Mininotes vous le rappelle.
- Sur le PC, les lignes avec du gras, de l'italique ou des liens ne sont plus dessinées les unes sur les autres.
- Sur le téléphone, ce qui arrive pendant que Mininotes est fermé est réuni dans une seule notification.
- *Horodaté sur Minima…* liste tout ce qui est horodaté, avec un + pour horodater autre chose.

### 0.4.016 à 0.4.018

- L'horodatage d'une note couvre son titre aussi bien que son texte.
- Temp est listé dans Récents quand vous l'utilisez.
- Le texte collé depuis d'autres apps garde ses retours à la ligne.
- Une image mise dans Temp arrive plus tôt sur vos autres appareils.

### 0.4.011 à 0.4.015

- Sur le téléphone, Réglages, *Le clavier apprend de mes notes* : activé, la saisie vocale du clavier fonctionne dans
  les notes.
- La clé Integritas va aux appareils que vous choisissez. Le menu *Cette note* d'une note a *Horodater sur Minima…*, un
  fichier horodaté est marqué, et les détails d'un horodatage renvoient au Minima Explorer.
- Moins de travail à chaque démarrage : les appareils ne se répètent plus tout.
- Le téléphone se remet à écouter de lui-même après un redémarrage ou une mise à jour.

### 0.4.010

- *Lisez-moi* devient **README** : sur l'accueil et sur GitHub, un seul et même texte, celui-ci. Un README que personne
  n'a modifié se réécrit de lui-même quand une nouvelle version en apporte un nouveau ; celui où vous avez écrit reste
  tel que vous l'avez fait.

### 0.4.008 à 0.4.009

- Un dossier album **Photos** sur l'accueil par défaut : vos images en grille à parcourir, chacune ouverte en grand avec
  la précédente et la suivante.
- Un résultat de recherche s'ouvre sur la première occurrence, avec une barre de recherche pour passer par chaque
  occurrence de la note.
- La conversation de l'**Agent** occupe toute la fenêtre, avec un bouton plein écran, et propose de vous emmener au bon
  endroit, par exemple la partie Integritas de *Mon profil*. Il garde la conversation quelques heures.
- *Lisez-moi* s'affiche ou se masque depuis *Afficher sur l'accueil*, avec sa propre icône.
- *Lisez-moi* décrit le contact d'urgence (la demande d'aide) et les limites des espaces privés.

### 0.4.007

- Une note sortie d'un dossier partagé sur l'un de vos appareils continue de se synchroniser avec vos autres appareils.
- Moins d'allers-retours entre appareils : la même réponse n'est plus envoyée encore et encore, et un téléphone
  travaille moins.
- Sur le PC, poser le pointeur sur une pastille à côté du titre d'une note indique de qui il s'agit, ce que cette
  personne peut faire avec la note, où elle en est de ce qui y est écrit, et combien de ses fichiers sont encore en
  route vers elle.

### 0.4.006

- La carte d'un PDF montre le haut de sa première page ; celle d'un fichier Word, PowerPoint, Excel, OpenDocument, RTF,
  HTML ou texte montre ses premières lignes.
- Sur le téléphone, *En choisir plusieurs…* sur un fichier maintenu coche plusieurs fichiers à envoyer d'un coup avec
  une autre application ou à mettre à la corbeille.
- *Mon profil* reçoit la clé API Integritas sur place, avec *Enregistrer*, et peut la transmettre à ceux de vos
  appareils que vous activez. *Horodaté sur Minima…* est passé dans le menu.
- Sur le PC, la taille du texte de *Mon profil* suit la même échelle A− et A+ que celle d'une note, et un clic droit
  dans Récents propose *Retirer des récents*. Le nom de l'app se trouve à gauche de la barre, à côté du logo.
- Les mots envoyés avec votre lien sont dans la langue que vous avez choisie.
- Les longues descriptions sous le pointeur s'affichent en entier, sur plusieurs lignes.

### 0.4.005

- Un fichier de note mis à la corbeille est retiré de la note sur chaque appareil et pour tous ceux à qui la note va.
  *Remettre* le ramène partout.
- Ce qui attend de partir ne compte que les fichiers que la note affiche, et les liste un par un.
- *Mon profil* a une partie Integritas, avec votre compte et la liste de ce que vous avez horodaté.
- Sur le PC, chaque entrée de menu a une courte description affichée sous le pointeur, activée ou non dans *Mon profil*.
- Sur le téléphone, *Copier* sur un fichier maintenu met le fichier lui-même dans le presse-papiers ; *Envoyer avec une
  autre application…* porte la marque de partage habituelle.
- Les réglages d'un groupe sont derrière son › ; *Mon profil* a *Gérer…* pour Personnes et appareils ; chaque entrée qui
  ouvre une fenêtre ou une page finit par « … » ; *Garder comme fichier* devient *Enregistrer sur mon appareil…*.
- La demande d'aide ne laisse aucune trace dans aucun menu : elle ne se règle que par un code tapé dans une note privée.
- Écrire dans une note partagée très active sur le PC ne rame plus.

### 0.4.004

- *Personnes et appareils* et *Mon profil* (autrefois *Profil*) sont à la fin de *Synchronisation et partage*.
- Sur le PC, *Mon profil* règle la taille du texte de toute l'app, en cinq crans, à partir du prochain démarrage.
- *Enregistrer sur mon ordinateur…* (sur le téléphone *Enregistrer sur mon téléphone…*) remplace *Télécharger* et
  *Enregistrer une copie…*. Chaque fenêtre qui choisit un fichier ou un dossier sur le PC est celle de Windows.
- La demande d'aide n'est plus que dans les espaces privés.

### 0.4.003

- **Horodater sur Minima** et **Vérifier sur Minima** avec votre propre compte Integritas.

### 0.4.002

- Sur le PC, *Copier* dans le menu d'un fichier met une image dans le presse-papiers comme image, et tout fichier comme
  fichier.
- Sur le téléphone, une ligne qui déroule un choix montre sa valeur à droite, sur une ligne.

### 0.4.001

- Sur le PC, le menu du trombone coche les fichiers d'une note pour agir sur eux ensemble, et propose *Prendre une
  photo* et *Enregistrer du son*.
- *Synchroniser un dossier de ce PC…* revient dans *Synchronisation et partage* : un dossier du PC, ses fichiers gardés
  dedans, affiché dans Mininotes avec sa propre marque.
- Chaque message d'erreur possible est traduit.

## 2. Installation

**Android** (9 ou plus récent) : téléchargez `Mininotes-<version>.apk` depuis
[la dernière version](https://github.com/mininotesorg/mininotes/releases/latest), vérifiez que c'est bien le fichier
publié, et ouvrez-le sur le téléphone :

```sh
sha256sum -c Mininotes-<version>.apk.sha256
```

Android demande s'il faut autoriser l'installation depuis l'endroit où vous l'avez téléchargé ; cette autorisation peut
être désactivée ensuite.

**Vérifiez qui l'a signé.** Chaque APK publié est signé avec la même clé Mininotes. Son empreinte de certificat SHA-256,
telle que [AppVerifier](https://github.com/soupslurpr/AppVerifier) la prend (copiez les deux lignes, puis *Verify from
clipboard*) :

```
org.mininotes.android
F7:91:C2:14:D9:ED:9B:E9:DE:EC:6F:15:BE:D0:25:38:35:22:DD:F9:A8:21:23:4A:73:90:C8:1B:60:1F:91:AB
```

Ou avec le SDK Android : `apksigner verify --print-certs Mininotes-<version>.apk`. Toute autre empreinte n'est pas une
version issue des publications de ce dépôt.

**Windows** (10 ou 11) : téléchargez `Mininotes-Windows-<version>.msi` depuis la même version et ouvrez-le. Il ne
demande aucun droit d'administrateur, et Mininotes figure ensuite dans les Paramètres de Windows, Applications, comme
n'importe quelle app. Le désinstaller là garde vos notes, qu'une installation ultérieure retrouve ;
*Désinstaller Mininotes…* dans le menu de l'app peut aussi les supprimer, après avoir proposé une sauvegarde. Java est
inclus ; rien d'autre n'est à installer.

Ou, pour qui le préfère, `Mininotes-Windows-<version>.zip` : extrayez-le où vous voulez et ouvrez `Mininotes.exe`.

Rien d'autre n'est nécessaire, ni sur l'un ni sur l'autre : le nœud Maxima qui transporte vos notes est dans l'app.

<!-- in the app again -->

## 3. Écrire

- **Une page lignée, prête.** Une note s'ouvre, le curseur dedans, et s'enregistre à mesure que vous écrivez, sans
  bouton pour enregistrer.
- **Onglets.** Une note peut contenir plusieurs pages, comme des fiches : *Ajouter un onglet*. Faites glisser un onglet
  pour le déplacer.
- **Gras, italique et souligné.** Tapez les marques autour des mots, `**gras**`, `*italique*` et `__souligné__`, ou
  faites Ctrl+B, Ctrl+I ou Ctrl+U.
- **Deux personnes dans une note.** Quand deux personnes écrivent dans la même note sans avoir vu le texte de l'autre,
  les deux sont réunis ligne par ligne et rien de ce qu'elles ont écrit n'est perdu. Le texte de chacun peut s'afficher
  dans sa couleur : touchez une pastille sous le titre pour en changer une.
- **Depuis d'autres apps.** Sur Android, Mininotes est dans le menu de partage : une capture d'écran, des photos, des
  fichiers, du texte ou un lien vont dans une nouvelle note ou une note récente.

## 4. Organiser : Accueil, dossiers, dock et favoris

- **Accueil** présente vos notes et dossiers en icônes, sur autant de pages que vous voulez, chacun où vous l'avez mis.
- **Créer.** Touchez + pour créer une note ou un dossier, ou ajouter des fichiers de cet appareil ou d'un autre.
- **Son menu.** Maintenez une icône pour son menu : couleur, icône, déplacement, partage, etc. Sur le PC, faites un clic
  droit ; le logo y ramène à l'accueil, et le séparateur se glisse pour redimensionner ou replier la liste latérale.
- **Les dossiers** contiennent des notes et d'autres dossiers, à tous les niveaux voulus. Glissez n'importe quoi sur un
  dossier pour l'y ranger.
- **Déplacer.** Maintenez une icône et glissez-la : dans un dossier, hors d'un dossier, ou vers une autre page. En
  chemin, la bande du haut permet de la lâcher sur un favori, Temp, les archives, la corbeille ou le partage, sans menu.
- **Pages.** Balayez d'une page à l'autre. Pincez l'accueil, ou choisissez *Toutes les pages* dans le menu, pour les
  voir toutes à la fois.
- **Couleurs et icônes.** Chaque note et dossier peut avoir sa couleur et son icône ou son image, depuis son menu.
  L'intensité de sa couleur se règle avec le curseur sous les couleurs. La couleur de l'accueil colore toute l'app.
- **Le dock** est la rangée en bas de l'accueil : ce que vous y mettez, à portée d'un toucher. *Ajouter au dock* est
  dans le menu de chaque note et dossier, sauf les favoris. Il en prend autant que vous voulez et défile de côté s'ils
  ne tiennent pas. Chaque appareil a son dock, jamais envoyé à vos autres appareils.
- **Les favoris** réunissent ce à quoi vous revenez. Un nouveau favori va aussi dans le dock ; retirer un favori le
  laisse dans le dock.
- **Récents** liste ce que vous avez ouvert dernièrement : sur le côté sur le PC, sur l'accueil sur le téléphone.
  *Retirer des récents* (clic droit sur le PC) retire un élément de la liste, sans rien supprimer. Sur le téléphone,
  balayez vers le haut depuis le dock, ou touchez la poignée juste au-dessus, pour feuilleter les récents en cartes :
  touchez-en une pour l'ouvrir, balayez-la ou faites Retour pour fermer.
- **Temp** sert à ce dont vous n'avez besoin qu'un temps, et à passer des choses à vos appareils et aux personnes
  choisies : son contenu leur parvient, puis disparaît partout à échéance.
- **Partagé avec moi** contient ce que d'autres vous ont partagé, en attente de votre réponse (voir
  [Partager avec d'autres personnes](#6-partager-avec-dautres-personnes)).
- **Rechercher**, en bas de l'accueil, trouve notes, dossiers et fichiers par leurs mots. **Arborescence** montre tout à
  la fois.
- **Archives et Corbeille** : à l'écart, ou sur le départ. On peut remettre des choses depuis les deux (voir
  [Versions, archives, corbeille et sauvegardes](#11-versions-archives-corbeille-et-sauvegardes)).
- **README**, sur l'accueil, est ce texte, celui de GitHub, sans ce qui ne sert que là-bas (sommaire, nouveautés,
  installation, compilation et liste des entrées de menu, que le PC indique sous le pointeur).
- **Afficher sur l'accueil**, dans *Réglages*, choisit ce que montre l'accueil (Récents, Temp, Partagé avec moi,
  archives, corbeille) et si le README y est. Un endroit désactivé reste dans le menu.
- **Photos** : un dossier album sur l'accueil par défaut, ses images en grille, chacune
  ouverte en grand, la suivante et la précédente à un balayage ou une flèche. Ajoutez des photos avec son +, ou
  prenez-en une. Tout dossier peut s'afficher en album depuis son menu.

## 5. Vos propres appareils

Reliez votre téléphone et votre PC une fois (*Personnes et appareils*, puis *Ajouter mon appareil*, et scannez le code
de l'un avec l'autre). Ensuite, ce que vous partagez avec vos appareils se met à jour tout seul sur chacun, dans les
deux sens, quelques secondes après que vous cessez d'écrire. Un de vos appareils peut être réglé sur *Peut lire* s'il
doit seulement consulter. Chaque appareil garde sa disposition : placer une note sur l'un ne la déplace pas sur l'autre,
et chacun a son dock.

## 6. Partager avec d'autres personnes

- **Ajouter quelqu'un** en scannant son code ou en ouvrant le lien qu'il vous envoie ; il fait de même avec le vôtre.
- **Partager une note, un dossier ou un fichier** depuis son menu, *Synchronisation et partage*, puis *Partager…*.
  Choisissez d'abord ce qu'ils pourront faire, puis une personne ou un groupe, ou montrez-leur votre code. Chacun reçoit
  un rôle : *Propriétaire* (qui l'a créé), *Administrateur*, *Peut écrire* ou *Peut lire*, à changer à tout moment dans
  la liste déroulante à côté, ou à retirer.
- **Depuis un autre appareil…**, sur n'importe quel +, reçoit ce que quelqu'un vous montre.
- **Les groupes**, comme Amis ou Collègues, réunissent des personnes : partagez avec un groupe et chaque membre reçoit
  le même rôle, même ajouté plus tard.
- **Accepter ou refuser.** Quand quelqu'un partage avec vous, une note vous prévient et l'élément attend dans *Partagé
  avec moi*. Vous voyez ce que c'est et qui l'a envoyé, et choisissez où il va, avant qu'il soit ailleurs. *Accepter*
  pour le garder, ou *Refuser*, discrètement ou avec un mot en retour.
- **Les couleurs voyagent.** La couleur d'une note partagée voyage avec elle ; chaque appareil règle son intensité.
- **Tout ce qui est dans un dossier partagé est partagé.** Ce que vous mettez dans un dossier partagé va aux personnes
  avec qui vous le partagez.
- **Les pastilles** à côté du titre d'une note sont les personnes et appareils qui l'ont, chacun avec son image ou son
  initiale. En toucher une affiche son image en grand. Sur le PC, le pointeur posé sur une pastille indique qui c'est,
  ce que la personne peut faire de la note, où elle en est de son texte et combien de ses fichiers sont encore en route
  vers elle.

## 7. Personnes et appareils, et Mon profil

Les deux sont à la fin de *Synchronisation et partage* dans le menu.

- **Personnes et appareils** liste les personnes ajoutées, vos appareils et vos groupes, chacun sur sa page. La page
  d'une personne montre ce que vous partagez avec elle et permet de lui donner une image de votre choix, vue de vous
  seul. Les réglages d'un groupe sont derrière son ›.
- **Le + à côté de Personnes** vous relie à quelqu'un : scannez son code, collez son lien, ou *Partager mon lien…* pour
  envoyer le vôtre dans un message ou une discussion. **Le + à côté de Mes appareils** ajoute un autre de vos appareils.
- **Mon profil** contient ce qui est à vous :
  - votre **nom** et votre **image**, qui vont aux personnes avec qui vous êtes associé ;
  - votre **adresse Parlons!**, envoyée d'elle-même à vos contacts ;
  - votre code et votre lien, pour que d'autres vous ajoutent ;
  - la clé API **Integritas** (voir [Horodater sur Minima avec Integritas](#13-horodater-sur-minima-avec-integritas)) ;
  - sur le PC, **Taille du texte** : une échelle A− et A+ de cinq crans, de Plus petit à Le plus grand, pour toute l'app
    (accueil, récents, menus, fenêtres), au prochain démarrage, avec *Redémarrer maintenant* ;
  - sur le PC, **Décrire ce que fait chaque chose**, activé par défaut, qui active ou non la courte ligne affichée sous
    le pointeur sur une entrée de menu ou une pastille ;
  - *Gérer…*, qui ouvre Personnes et appareils.

## 8. Savoir où en est chaque chose

- **La marque** sur chaque note, dossier et icône dit où il en est : un anneau gris vide, seulement sur cet appareil ;
  une flèche orange, en attente de départ (appuyez pour envoyer tout de suite, et voir chaque note et fichier en
  attente) ; trois points orange, envoyé, attend une réponse ; une coche verte, tout le monde a cette version ; deux
  barres grises, en pause ; un point d'exclamation rouge, un appareil sans contact depuis plus de trois jours.
- **Nouveau** liste ce qui a changé depuis votre dernier passage : notes où d'autres ont écrit, éléments partagés avec
  vous, fichiers arrivés, avec l'expéditeur, la date et l'endroit de chacun.
- **La bande du haut** apparaît en cas de problème avec un contact (un appareil injoignable depuis des jours,
  verrouillé, ou avec un Mininotes plus ancien). Mininotes réessaie de lui-même, et *Que faire* dit quoi faire.

## 9. Fichiers, photos et enregistrements

- **Joindre** n'importe quel fichier à une note ou à un dossier, plusieurs à la fois.
- **Prendre une photo** depuis une note : l'appareil photo du téléphone, ou sur le PC l'app Caméra de Windows, dont les
  photos arrivent droit dans la note.
- **Enregistrer du son** dans une note.
- **Léger à transporter.** Les photos sont réduites à l'ajout : une note de cinquante photos reste légère ; cela se
  désactive dans *Réglages*, *Images et fichiers*. Les autres sont compressés pour gagner de la place.
- **Aperçus.** La carte d'un PDF montre sa première page ; celle d'un fichier Word, PowerPoint, Excel, OpenDocument,
  RTF, HTML ou texte, ses premières lignes. Mininotes les dessine depuis le fichier, jamais Windows ni une autre app :
  aucune image n'en reste ailleurs.
- **Ouvrir** un fichier dans le programme habituel de l'ordinateur (*Ouvrir dans LibreOffice*, par exemple), ou un autre
  avec *Ouvrir avec…*. Les modifications enregistrées reviennent dans Mininotes et vont à vos appareils.
- **Copier**, sur un fichier (clic droit sur le PC, appui long sur le téléphone), met le fichier lui-même dans le
  presse-papiers, une image comme image, à coller ailleurs sans l'enregistrer d'abord.
- **L'enregistrer ailleurs.** *Enregistrer sur mon ordinateur…* (sur le téléphone *Enregistrer sur mon téléphone…*)
  enregistre une copie où vous voulez. *Enregistrer sur mon appareil…* le garde aussi comme fichier dans un dossier de
  cet appareil.
- **Envoyer avec une autre application…**, sur le téléphone, ouvre le partage du téléphone, pour passer par WhatsApp,
  par exemple. *En choisir plusieurs…* sur un fichier maintenu en coche plusieurs à envoyer ainsi ensemble ou à mettre à
  la corbeille.
- **Plusieurs à la fois sur le PC.** Le menu du trombone propose *Joindre un fichier*, *Prendre une photo* et
  *Enregistrer du son*, puis les fichiers de la note, chacun avec sa case : *Tout sélectionner*, puis *Enregistrer sur
  mon ordinateur…* (plusieurs vont dans un même dossier), *Horodater sur Minima…* ou *Mettre à la corbeille* pour les
  fichiers cochés.
- **Envoyer des fichiers** directement à un autre de vos appareils ou à quelqu'un d'ajouté, hors de toute note, de porte
  à porte sur le même Wi-Fi. Ce qui arrive va sur l'accueil, marqué *nouveau* jusqu'à son ouverture.
- **Horodater sur Minima…** (dans le menu d'un fichier ou d'une note) : voir
  [Horodater sur Minima avec Integritas](#13-horodater-sur-minima-avec-integritas).

## 10. Les fichiers qui restent sur votre PC

- **Partager un fichier via Mininotes** (sur le PC, dans *Synchronisation et partage*) : un tableur ou un document reste
  où il est sur votre PC et s'ouvre dans son programme. À chaque enregistrement, cette version va à vos autres
  appareils, et une version enregistrée ailleurs y est réécrite. Le fichier entier voyage, en dernière version : les
  modifications d'un tableur ne sont pas fusionnées. S'il a changé à deux endroits avant qu'ils se rejoignent, Mininotes
  demande quelle version garder et ne remplace rien avant votre réponse.
- **Synchroniser un dossier de ce PC** (sur le PC, dans *Synchronisation et partage*) : choisissez un dossier, sur C: ou
  ailleurs, et son contenu apparaît dans Mininotes en dossier marqué comme vivant sur votre PC. Les fichiers que vous y
  mettez, dans l'Explorateur ou depuis un programme, sont pris quelques secondes plus tard et vont à vos appareils et à
  ceux avec qui vous partagez le dossier ; les leurs y sont écrits. Ils restent dans ce dossier, hors de l'app. En
  supprimer un sur le disque ne le supprime jamais dans Mininotes, ni l'inverse ; si un fichier a changé aux deux
  endroits, Mininotes demande quelle version garder. Sur un téléphone, les fichiers du dossier sont gardés dans l'app.
- **Les notes comme fichiers.** Une note ou un dossier peut aussi être gardé en fichiers Markdown dans un dossier de
  votre choix sur le PC (*Enregistrer sur mon appareil…*). Mininotes ne supprime jamais ces fichiers et n'y écrit jamais
  une note privée.
- Un tel dossier porte un dossier fléché sur sa tuile. Son menu indique où il se trouve sur le PC, puis *Afficher dans
  l'Explorateur*, *Garder seulement dans l'app* et *Retirer de l'app*.

## 11. Versions, archives, corbeille et sauvegardes

- **Versions.** Chaque version d'une note est gardée ; *Versions…* en remet une, au choix.
- **Archives** range des choses hors de vue ; la **corbeille** garde ce que vous avez supprimé, fichiers compris,
  jusqu'à ce que vous la vidiez. Les deux sont sur l'accueil, avec un retour. *Remettre* remet une chose où elle était.
- **Un fichier de note à la corbeille.** Un fichier retiré d'une note va aussitôt à la corbeille, sans question, et
  quitte la note sur tous vos appareils et pour tous ceux à qui elle va. *Remettre* le ramène partout. Il ne disparaît
  pour de bon qu'une fois la corbeille vidée.
- **Sauvegardes.** *Exporter une sauvegarde* écrit un seul fichier avec chaque dossier, note et fichier. Sans le verrou
  (voir [Confidentialité, chiffrement et sécurité](#14-confidentialité-chiffrement-et-sécurité)), il demande un mot de
  passe pour cette sauvegarde. *Ajouter depuis une sauvegarde* en restaure une, en ajout à l'existant ou à sa place, au
  choix.

## 12. L'Agent : demandez, et c'est fait

L'**Agent** (barre du haut sur le PC, à côté du menu sur le téléphone, ou Ctrl+K) répond aux questions sur Mininotes
d'après l'aide intégrée à l'app ([aide en anglais](android/app/src/main/resources/org/mininotes/android/help/en.txt),
[en français](android/app/src/main/resources/org/mininotes/android/help/fr.txt)), et fait des tâches simples dites en
anglais ou en français : *quoi de neuf ?*, *qu'y a-t-il sur ma liste ?*, *ajoute du lait à Courses*, *partage Courses
avec Carole*, *ajoute un contact*, *ouvre*, *cherche*, *nouvelle note appelée…*, *mets … dans le dock*. Il tourne sur
votre appareil sans modèle ni serveur, n'envoie rien, montre avant de le faire tout ce qui parvient à d'autres ou
modifie une note, et propose de vous emmener au bon endroit. Un appui hors de sa fenêtre le ferme.

## 13. Horodater sur Minima avec Integritas

Avec un compte [Integritas](https://docs.integritas.technology/), un fichier ou une note peut être horodaté sur la
blockchain Minima depuis son menu. Seule son empreinte (SHA-256) quitte l'appareil, jamais le fichier.

- **Horodater sur Minima…** inscrit maintenant que le fichier existe exactement tel quel : une preuve, dès ce moment,
  qu'il existait et n'a pas changé depuis. Cela utilise un horodatage de votre compte Integritas.
- **Vérifier sur Minima…** compare plus tard un fichier à ce qui a été inscrit : horodaté ou non, inchangé ou non, dans
  quel bloc et à quelle date. Cela ne change rien et ne coûte rien.
- **La différence :** horodater écrit la preuve une fois ; vérifier la relit, aussi souvent que vous voulez.
- **L'horodatage d'une note** couvre son titre, une ligne vide, puis son texte ; la renommer la marque comme modifiée.
  Les horodatages faits avant 0.4.018 ne couvrent que le texte : *Note, texte seul*.
- **Mon profil, Horodatage :** collez votre clé API et appuyez sur *Enregistrer* ; elle est gardée dans votre carnet,
  verrouillée avec lui, jamais dans une sauvegarde. Activez vos appareils sous *Aussi sur mes appareils* et la clé leur
  parvient aussi, scellée pour chacun.
- **Horodaté sur Minima…**, en dernier dans *Chercher*, liste ce qui a été horodaté ici ; ouvrez-en un pour son
  empreinte, son bloc et sa transaction. Son **+** horodate une note ou un fichier de Mininotes, ou un fichier de cet
  appareil hors de Mininotes.

## 14. Confidentialité, chiffrement et sécurité

- **Ni serveur, ni compte, ni pistage.** Les notes restent sur vos appareils et ne vont qu'à ceux avec qui vous les
  partagez.
- **Scellé de bout en bout.** Chaque note et fichier est scellé pour l'appareil qui le reçoit, puis transporté par des
  relais [Maxima](https://minima.global), qui relaient des messages illisibles pour eux, ou directement d'un appareil à
  l'autre sur le même réseau.
- **Verrouiller Mininotes** (*Réglages*, *Sécurité*) chiffre le carnet, ses fichiers et vos sauvegardes sur l'appareil.
  Il s'ouvre par empreinte digitale ou verrouillage d'écran sur le téléphone, par Windows Hello ou mot de passe sur le
  PC, avec 12 mots de récupération en clé de secours : notez-les. Sans lui, les notes ne sont pas chiffrées sur
  l'appareil.
- **Deux choses utilisent le réseau :** le nœud Maxima qui transporte ce que vous partagez, et un coup d'œil quotidien à
  ce dépôt pour une version plus récente, désactivable. [docs/SHARING.md](docs/SHARING.md) dit ce qui est scellé, ce qui
  est signé, et ce qui n'est pas encore construit.

## 15. Espaces privés et demande d'aide

**Espaces privés**

- **En ouvrir ou en créer un.** Dans n'importe quelle note, tapez le mot private, puis //: et votre mot de passe, le
  tout collé, sans rien entre, et appuyez sur Entrée. Le code quitte la note dès les deux-points tapés, et le mot de
  passe se tape dans une fenêtre à part : aucun des deux ne reste dans la note. L'espace s'ouvre sur un écran à lui,
  par-dessus toute la fenêtre. Un mot de passe jamais utilisé se tape deux fois, et un nouvel espace vide est créé.
- **Un espace par mot de passe.** Chaque mot de passe différent ouvre un espace distinct. Créez-en autant que vous
  voulez : rien nulle part ne note combien vous en avez.
- **Notez chaque mot de passe.** Comme rien ne les garde, un mot de passe mal tapé est pris pour un nouvel espace et
  redemandé : si vous vouliez un espace existant, fermez la fenêtre et réessayez. Un nouvel espace peut recouvrir un
  ancien et en faire perdre une partie.
- **Où vous êtes.** L'écran privé a sa couleur et un cadenas à côté de *Privé* : vous savez toujours où vous êtes. Créez
  notes, dossiers et fichiers avec le + en bas. L'œil près du mot de passe l'affiche tant que vous le maintenez. Retour
  ferme l'espace.
- **En supprimer un.** Ouvrez-le, puis *Supprimer cet espace privé…* dans son menu. Il disparaît pour de bon et son mot
  de passe n'ouvre plus rien.
- **Un long mot de passe.** Prenez une longue phrase de plusieurs mots. Nul ne peut retrouver un mot de passe oublié,
  même avec les 12 mots de récupération.
- **Ce qu'il contient.** Environ 64 Mo en tout, assez pour des notes et une cinquantaine de photos. Son contenu
  n'apparaît nulle part ailleurs, reste pour l'instant sur cet appareil seul, ne se partage ni ne s'envoie, et se ferme
  quand vous quittez Mininotes ou après cinq minutes d'inactivité.
- **Un code n'est jamais gardé dans une note ordinaire.** S'il en reste un, Mininotes le retire, avec le reste de sa
  ligne, avant que la note soit enregistrée, envoyée ou partagée.
- **Fichiers.** *Depuis cet appareil…* sur le + ajoute un fichier dans l'espace, où il est gardé chiffré. L'original
  reste en place sauf si vous le supprimez, et le sélecteur de fichiers du téléphone garde sa liste des fichiers
  récents.
- **Astuce.** Gardez un espace crédible sous un mot de passe et le vrai sous un autre, et utilisez le crédible de temps
  en temps pour qu'il ait l'air habité.
- **Leurs limites.** Les espaces privés sont conçus pour que nul, en lisant les fichiers de l'app ou son code, ne puisse
  les voir ni les compter. Rien ne garantit une sûreté à 100 % : n'y gardez que ce qui ne vous nuirait pas si on le
  trouvait.

**Contact d'urgence**

- **Ce que c'est.** Une demande d'aide envoie votre position, et les mots choisis, aux personnes choisies, dès qu'une
  note privée est ouverte. Elle sert si vous êtes peut-être en danger et qu'on pourrait vous forcer à ouvrir Mininotes.
- **En régler une.** Dans une note d'un espace privé, tapez le mot help, puis //:, le tout collé, et appuyez sur Entrée.
  Le code disparaît aussitôt et une fenêtre demande qui prévenir et quoi dire. C'est la note où vous l'avez tapé qui
  l'envoie. Seul qui connaît le mot de passe de l'espace peut en régler une ou la retirer.
- **Plus jamais affichée.** Une fois réglée, elle ne se montre plus nulle part : aucun menu n'en parle, et la fenêtre
  s'ouvre toujours vide.
- **Les retirer.** Pour retirer toutes les demandes d'un espace, tapez le mot erase, puis //:, dans l'une de ses notes.
  Ce code aussi disparaît aussitôt.
- **Position.** Mininotes demande pour cela l'autorisation de localisation du téléphone, et le système signale son
  usage.

## 16. Langues et traduction

Mininotes parle onze langues : anglais, français, allemand, espagnol, italien, portugais, russe, chinois, japonais,
hindi et persan. Il demande laquelle au premier démarrage, celle de l'appareil présélectionnée, et ce README est aussi
dans cette langue ; *Réglages, Langue* change les deux. *Traduire…* dans le menu d'une note traduit la page sur
l'appareil, sans l'envoyer à un service de traduction.

<!-- not in the app -->

## 17. Chaque entrée de menu, en une ligne

Sur le PC, poser le pointeur sur une entrée de menu indique ce qu'elle fait. *Mon profil*, *Taille du texte*, *Décrire
ce que fait chaque chose* active ou désactive ces lignes. Une entrée qui finit par « … » ouvre une fenêtre ou une page ;
une entrée sans le fait tout de suite.

| Entrée | Ce qu'elle fait |
|---|---|
| Renommer | Donnez-lui un autre nom. |
| Retirer cet onglet | Retirez cet onglet de la note. |
| Déplacer à gauche | Placez cet onglet une place plus à gauche. |
| Déplacer à droite | Placez cet onglet une place plus à droite. |
| Choisir une image | Affichez dessus une image à vous à la place de son icône. |
| Retirer l'image | Réaffichez son icône à la place de l'image. |
| Image | Choisissez ou retirez une image à vous pour lui. |
| Icône | Choisissez l'icône qu'il porte sur l'accueil et dans les listes. |
| Couleur | Colorez-le, avec l'intensité de votre choix. |
| Couleur d'écriture | La couleur dans laquelle s'affiche ce qu'écrit cette personne. |
| Partage | Qui l'a et ce que chacun peut faire. |
| Partager | Partagez-le avec des personnes ou vos appareils, et choisissez ce que chacun peut faire. |
| Paramètres de synchronisation | Quand et comment ses changements vont aux autres. |
| Synchroniser maintenant | Envoyez maintenant ce qui attend, et demandez aux autres le leur. |
| Afficher mon code | Un code à scanner par un autre appareil ou une personne, pour le recevoir. |
| Partager un fichier via Mininotes | Synchronisez un fichier de ce PC avec vos appareils. Il reste où il est. |
| Synchroniser un dossier de ce PC | Synchronisez tout un dossier de ce PC avec vos appareils. Ses fichiers y restent. |
| Personnes et appareils | Les personnes et appareils avec qui vous partagez, et vos groupes. |
| Mon profil | Votre nom, votre code, votre connexion et les réglages de l'app qui sont les vôtres. |
| Depuis un autre appareil | Scannez un code ou collez un lien venant d'un autre appareil ou d'une personne. |
| Scanner un code ou coller leur lien | Ajoutez quelqu'un, ou un de vos appareils, à partir de son code ou de son lien. |
| Copier mon lien | Copiez votre lien, à envoyer à quelqu'un pour qu'il puisse vous ajouter. |
| Joindre un fichier | Gardez un fichier de ce PC avec cette note. |
| Ajouter un fichier | Gardez un fichier de ce PC dans ce dossier. |
| Prendre une photo | Ouvrez l'appareil photo : la photo arrive dans cette note. |
| Afficher en album | Affiche ses images en grille, pour les parcourir en grand. |
| Ajouter des photos | Ajoutez plusieurs images de cet appareil à la fois. |
| Enregistrer du son | Enregistrez du son dans cette note. |
| Pièces jointes | Les fichiers gardés avec cette note. |
| Déplacer vers | Mettez-le dans un autre dossier, ou sur l'accueil. |
| Ajouter au dock | Mettez-le dans la rangée en bas de l'accueil, à un clic. |
| Retirer du dock | Retirez-le du dock. Rien n'est supprimé. |
| Ajouter aux favoris | Gardez-le parmi vos favoris, et dans le dock. |
| Copier le texte | Copiez le texte de la note, pour le coller ailleurs. |
| Traduire | Traduisez cette page dans une autre langue, sur cet appareil. |
| Versions | Toutes les versions précédentes de cette note, à consulter ou à remettre. |
| Horodater sur Minima | Inscrivez maintenant sur la blockchain Minima qu'il existe tel qu'il est. Seule son empreinte quitte cet appareil. |
| Vérifier sur Minima | Vérifiez s'il est sur Minima sans changement, et quand il a été inscrit. |
| Temporaire | Gardez-le seulement un temps : il s'en va quand son temps est écoulé. |
| Archives | Rangez-le, hors de vue, conservé. |
| Mettre à la corbeille | Mettez-le à la corbeille. Il peut être remis jusqu'à ce que la corbeille soit vidée. |
| Mettre à la corbeille | Mettez-le à la corbeille. Il peut être remis jusqu'à ce que la corbeille soit vidée. |
| Nouvelle note | Une nouvelle note, ici. |
| Nouveau dossier | Un nouveau dossier, ici. |
| Nouveau dossier dans | Un nouveau dossier dans celui-ci. |
| Toutes les pages | Toutes les pages de l'accueil à la fois. |
| Retour à la page principale | Retour à la première page de l'accueil. |
| Ouvrir le lien | Ouvrez ce lien dans votre navigateur. |
| Copier le lien | Copiez ce lien. |
| Couper | Coupez le texte choisi. |
| Copier | Copiez-le, pour le coller ailleurs : une image comme image, un fichier comme fichier. |
| Coller | Collez ce qui a été copié. |
| Tout sélectionner | Sélectionnez tout. |
| Annuler | Annulez la dernière modification. |
| Lignes d'écriture | Affichez ou masquez les lignes de la page. |
| Récents | Ce que vous avez ouvert récemment. |
| Temp | Ce qui n'est gardé qu'un temps, et va à vos autres appareils. |
| Partagé avec moi | Ce que d'autres vous ont partagé, en attente de votre réponse. |
| Corbeille | Ce qui a été mis à la corbeille, à remettre ou à supprimer définitivement. |
| Écriture de récupération | Ce qui a été écrit sans pouvoir être enregistré dans sa note, mis de côté. |
| Exporter une sauvegarde | Écrivez un seul fichier contenant chaque dossier, note et fichier. |
| Ajouter depuis une sauvegarde | Restaurez le contenu d'un fichier de sauvegarde. |
| Partager Mininotes | Parlez de Mininotes à quelqu'un. |
| Mettre à jour vers la v | Installez la version plus récente. |
| Avis | Dites quelque chose sur Mininotes, publiquement, sous votre nom. |
| Réglages | Comment Mininotes fonctionne sur cet appareil. |
| Mininotes sur votre téléphone | Mininotes sur les téléphones Android, avec un code à scanner pour l'obtenir. |
| Mininotes sur votre PC | Mininotes sous Windows, avec le lien pour l'obtenir. |
| À propos | Quelle version c'est, et qui a fait Mininotes. |
| Désinstaller Mininotes | Retire Mininotes de ce PC, en gardant vos notes ou non, après avoir proposé une sauvegarde. |
| Quitter Mininotes | Fermez Mininotes. |
| Envoyer des fichiers | Envoyez des fichiers directement à un de vos appareils ou à une personne, hors de toute note. |
| Fichiers envoyés | Les fichiers envoyés d'ici, et où en est chacun. |
| Ouvrir un fichier Markdown | Ouvrez un fichier Markdown de ce PC comme une note gardée dans ce fichier. |
| Modifier un fichier | Ouvrez n'importe quel fichier texte de ce PC pour le modifier, sans le garder dans Mininotes. |
| Rechercher | Trouvez des notes, dossiers et fichiers par leurs mots. |
| Agent | Posez une question, ou demandez que quelque chose soit fait. |
| Arborescence | Tous les dossiers et notes à la fois, sur le côté. |
| Dock | Affichez ou masquez le dock sur l'accueil. |
| Menu | Tout ce que Mininotes sait faire. |
| Durée de conservation | Combien de temps ce qui est dans Temp est gardé. |
| Langue | La langue de l'app. |
| Envoyer le contenu de Temp | Donnez ce qui est dans Temp à vos autres appareils. |
| À qui il va | Lesquels de vos appareils et personnes reçoivent ce qui est dans Temp. |
| Afficher aussi sur l'accueil | Affichez aussi sur l'accueil ce qui est dans Temp. |
| Synchroniser Temp maintenant | Envoyez maintenant ce qui est dans Temp. |
| Enregistrer sur mon ordinateur | Enregistrez une copie à l'endroit de votre choix sur ce PC. |
| Enregistrer sur mon téléphone | Enregistrez une copie à l'endroit de votre choix sur ce téléphone. |
| Enregistrer sur mon appareil | Gardez-le aussi comme fichier dans un dossier de cet appareil. |
| Retirer | Retirez-le. |
| Ouvrir | Ouvrez-le. |
| Ouvrir avec | Ouvrez-le dans un autre programme. |
| Envoyer à un appareil | Envoyez-le directement à un de vos appareils ou à une personne. |
| Depuis cet appareil | Ajoutez des fichiers de cet appareil. |
| Quelle version garder | Il a changé à deux endroits : choisissez quelle version reste. |
| Afficher dans l'Explorateur | Montrez où il se trouve sur ce PC. |
| Synchronisé avec le fichier de ce PC | Synchronisez-le avec son fichier sur ce PC, ou arrêtez. |
| Remplacer par un autre fichier | Mettez un autre fichier à sa place. |
| Mettre dans une note | Gardez-le avec une note. |
| Retirer de Mininotes | Retirez-le de Mininotes. Le fichier sur ce PC reste. |
| Vider la corbeille | Supprimez définitivement tout ce qui est dans la corbeille. |
| Masquer de l'accueil | Ne l'affichez pas sur l'accueil. Il reste dans le menu. |
| En attente | Ce qui attend de partir, et pourquoi. |
| Retirer des récents | Retirez-le de la liste de ce que vous avez ouvert récemment. |
| Remettre | Remettez-le où il était. |
| Supprimer définitivement | Supprimez-le définitivement. Il ne pourra pas être remis. |
| Taille du texte | La taille à laquelle le texte s'affiche. |
| Comme les autres notes | Affichez cette note à la taille habituelle. |
| Le retrouver | Indiquez à Mininotes où se trouve maintenant son fichier. |
| Garder seulement dans l'app | Arrêtez de le garder comme fichier sur cet appareil. Il reste dans Mininotes. |
| Retirer de l'app | Retirez-le de Mininotes. Son fichier sur cet appareil reste. |
| Supprimer le groupe | Supprimez ce groupe. Les personnes qui en font partie restent. |
| Mon appareil | Comptez-le comme un de vos propres appareils. |
| Oublier | Retirez cet appareil de votre liste. Aucun partage n'est retiré. |
| Retirer cette personne | Retirez cette personne de tout ce que vous lui avez partagé, et de vos personnes. |
| Changer le mot de passe | Un nouveau mot de passe pour votre carnet. |
| Supprimer | Supprimez-le. |
| Cette note | Ce qu'on peut faire avec la note ouverte. |

<!-- in the app again -->

## 18. Mises à jour

L'app cherche une nouvelle version une fois par jour et le dit en une ligne ; *Mettre à jour* l'installe. La
[dernière version](https://github.com/mininotesorg/mininotes/releases/latest) a toujours les fichiers.

## 19. Donnez votre avis

*Avis*, dans le menu de l'app, ouvre ici un formulaire prérempli, sous votre propre nom ; rien n'est publié à votre
place. Tous les avis sont dans
[les tickets d'avis](https://github.com/mininotesorg/mininotes/issues?q=is%3Aissue+label%3Afeedback). Un problème de
**sécurité** se signale en privé, par
[un avis de sécurité](https://github.com/mininotesorg/mininotes/security/advisories/new), pas dans un ticket.
[docs/FEEDBACK.md](docs/FEEDBACK.md) dit le reste.

<!-- not in the app -->

## 20. Le compiler vous-même

**Android** demande le JDK 17 et le SDK Android (compileSdk 36) :

```sh
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

L'APK arrive dans `android/app/build/outputs/apk/release/`, non signé : signez-le avec votre propre clé, ou utilisez
`./gradlew installDebug`. `android/local.properties` pointe vers votre SDK et n'est pas dans le dépôt.

**Windows** demande le JDK 17 : lancez `./windows/build.ps1 -Package` et ouvrez le `Mininotes.exe` qui se trouve dans
`dist/latest/Mininotes-Windows-<version>/`. Voir [windows/README.md](windows/README.md).

**Vérifications :** environ 870 tests unitaires sur Android et 1 360 sur Windows, sans aucun appareil. Ils couvrent les
points où une erreur ferait perdre ce que quelqu'un a écrit : la fusion, les notes qui arrivent, qui peut faire quoi,
les migrations de schéma, l'association, les pièces jointes. Ce qui demande un vrai appareil est vérifié sur deux
téléphones et un PC avant chaque version, dans un journal que le mainteneur garde en privé, parce qu'il nomme ses
appareils et cite ses notes. Demandez dans un ticket l'entrée qui correspond à une version.

## 21. Le contenu de ce dépôt

| Chemin | Ce qu'il contient |
| --- | --- |
| [android/](android/) | L'app Android, et le code que les deux apps partagent. Pas de mises en page XML : les vues sont construites en Java. |
| [windows/](windows/README.md) | L'app Windows et son empaquetage. |
| [docs/HOME.md](docs/HOME.md) | L'organisation de l'app, et chaque décision prise, numérotée. |
| [docs/SHARING.md](docs/SHARING.md) | La conception de la synchronisation : clés, association, fusion, conflits. |
| [docs/PRODUCT.md](docs/PRODUCT.md) | À quoi sert l'app, et ce qu'elle n'est pas. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Comment aider, et par où commencer. |

Peu de dépendances tierces à l'exécution : ZXing pour les codes QR, SQLCipher avec AndroidX SQLite pour le carnet
chiffré, FlatLaf pour l'apparence sous Windows, PDFBox pour la première page d'un PDF sur le PC. C'est voulu ainsi, pour
que l'ensemble puisse être lu.

<!-- in the app again -->

## 22. Licence

Mininotes est un **logiciel libre et open source** sous la
[GNU General Public License, version 3 ou ultérieure](LICENSE). Vous pouvez l'utiliser, l'étudier, le modifier, le
transmettre et le vendre, mais ce que vous transmettez, modifié ou non, part avec son code source sous la même licence.
Les conditions des tiers sont dans [NOTICE](NOTICE).

Le transport Maxima intégré à l'app est le cœur de [eurobuddha/maxima](https://github.com/eurobuddha/maxima), inclus
sans modification avec la permission de son auteur et crédité dans NOTICE. Il lui appartient et n'est pas sous GPL ; son
propre dépôt ne porte pas encore de licence.

<!-- not in the app -->

Jusqu'au 26 septembre 2026, Mininotes était à source disponible (Apache 2.0 avec la Commons Clause et une condition
visant les produits payants) ; les copies prises avant cette date gardent ces conditions.

Contributions : [CONTRIBUTING.md](CONTRIBUTING.md). Signalements de sécurité : [SECURITY.md](SECURITY.md).
