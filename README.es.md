<!-- Translated from README.md, SHA-256 a78f7778ee3e66851fb3a075691b87fc51902d2d0cebd76aceca20322bba7a36 -->

# Mininotes

Mininotes es un cuaderno para tu teléfono y tu PC que mantiene tus notas, carpetas y archivos iguales en todos tus
dispositivos y los comparte con las personas que elijas. No hay servidor, ni cuenta, ni nadie en medio: todo viaja
sellado de extremo a extremo por la red Minima, o directamente de dispositivo a dispositivo en la misma wifi.

[![Última versión](https://img.shields.io/github/v/release/mininotesorg/mininotes?label=latest%20build)](https://github.com/mininotesorg/mininotes/releases/latest)

<!-- not in the app -->

[中文](README.zh.md) · [English](README.md) · [Français](README.fr.md) · [Deutsch](README.de.md) ·
[Português](README.pt.md) · [Italiano](README.it.md) · [日本語](README.ja.md) · [Русский](README.ru.md) ·
[हिन्दी](README.hi.md) · [فارسی](README.fa.md)

## Contenido

1. [Novedades](#1-novedades)
2. [Instalarlo](#2-instalarlo)
3. [Escribir](#3-escribir)
4. [Organizar: Inicio, carpetas, dock y favoritos](#4-organizar-inicio-carpetas-dock-y-favoritos)
5. [Tus propios dispositivos](#5-tus-propios-dispositivos)
6. [Compartir con otras personas](#6-compartir-con-otras-personas)
7. [Personas y dispositivos, y Mi perfil](#7-personas-y-dispositivos-y-mi-perfil)
8. [Saber cómo está todo](#8-saber-cómo-está-todo)
9. [Archivos, fotos y grabaciones](#9-archivos-fotos-y-grabaciones)
10. [Archivos que se quedan en tu PC](#10-archivos-que-se-quedan-en-tu-pc)
11. [Versiones, archivados, papelera y copias de seguridad](#11-versiones-archivados-papelera-y-copias-de-seguridad)
12. [El Agente: pide, y se hace](#12-el-agente-pide-y-se-hace)
13. [Sellar en Minima con Integritas](#13-sellar-en-minima-con-integritas)
14. [Privacidad, cifrado y seguridad](#14-privacidad-cifrado-y-seguridad)
15. [Espacios privados y la petición de ayuda](#15-espacios-privados-y-la-petición-de-ayuda)
16. [Idiomas y traducción](#16-idiomas-y-traducción)
17. [Cada entrada del menú, en una línea](#17-cada-entrada-del-menú-en-una-línea)
18. [Actualizaciones](#18-actualizaciones)
19. [Di algo sobre Mininotes](#19-di-algo-sobre-mininotes)
20. [Compílalo tú mismo](#20-compílalo-tú-mismo)
21. [Qué hay en este repositorio](#21-qué-hay-en-este-repositorio)
22. [Licencia](#22-licencia)

---

## 1. Novedades

Lo más reciente primero. Cada versión es igual en el teléfono y en el PC salvo que se indique otra cosa.

### 0.4.030

- *Personas y dispositivos* tiene un solo + redondo que abre la cámara: escanea el código del otro dispositivo, luego di
  qué es (uno de tus dispositivos u otra persona), ponlo en un grupo si quieres y elige qué recibe. *Mostrar mi código*
  está al lado.
- Un teléfono que vuelve tras estar fuera ya no se ralentiza: lo que se guardó para él mientras no estaba llega una sola
  vez y se responde una sola vez.

### 0.4.029

- *Añadir mi dispositivo* pregunta primero qué recibe el dispositivo nuevo: *Nada aún*, o todo para leer o para
  escribir. Con *Nada aún* es uno de tus dispositivos, recibe lo que después compartas con él, y eliges en su línea de
  *Personas y dispositivos*. Su ventana se abre al instante.
- Los mensajes hechos con nombres, como una nota que no pudo llegar a un dispositivo o lo que ofrece un código de
  vinculación, están en tu idioma, en el dispositivo que los muestra.

### 0.4.028

- Una nota o un archivo sellado que compartes se lleva su sello: la persona con quien lo compartes lo ve en *Sellar en
  Minima…*, como el sello de lo que recibió solo si es exactamente lo mismo, con los enlaces para comprobarlo en el
  Minima Explorer. Un sello hecho después de compartir también le llega.
- Cuando compartes un archivo cuyo informe de Integritas se guarda al lado, *Enviar también su informe de Integritas*
  envía también el informe.
- La ventana de idiomas del primer inicio está en inglés, con los idiomas en orden alfabético.

### 0.4.024 a 0.4.027

- **Sellar en Minima…** es una sola línea en el menú de una nota o de un archivo, que dice en qué punto está su sello, y
  abre una sola ventana con todo: sellar, buscarlo en Minima, verificar de nuevo, las versiones selladas antes y
  comparar un archivo con una huella. El informe PDF de Integritas puede adjuntarse a la nota o guardarse al lado del
  archivo.
- Los sellos van a tus otros dispositivos: lo sellado en el teléfono aparece en la lista del PC, y al revés.
- Un archivo sellado lleva un pequeño sello en su icono.
- *Comparar con una huella…*: pega el SHA-256 que te dieron para un archivo y ve al instante si es el mismo archivo.
  Nada sale del dispositivo y no hace falta ninguna cuenta.
- Lo que responde Integritas se dice en tu idioma, y cuando no responde enseguida, se le vuelve a preguntar.
- En el PC, las palabras de cada pantalla, botón y descripción bajo el puntero están en tu idioma; las primeras carpetas
  de un cuaderno nuevo también llevan nombres en él. En francés, una nota nueva lleva *nouv.*
- El teléfono ya no deja de responder cuando su pantalla se vuelve a dibujar mientras aún intenta llegar a los relés.

### 0.4.021 a 0.4.023

- Las pestañas de una nota cambian de orden arrastrándolas en el PC, o manteniéndolas pulsadas y llevándolas en el
  teléfono; su menú también tiene *Mover a la izquierda* y *Mover a la derecha*.
- El idioma se pregunta en cada inicio hasta que se confirma uno; en el teléfono basta con tocar un idioma.
- Cada app presenta a la otra: *Mininotes en tu móvil…* en el PC, *Mininotes en tu PC…* en el teléfono, con un código
  para escanear.
- El instalador de Windows muestra el logo de Mininotes y puede abrir Mininotes al terminar.
- Una nota empezada en Temp se queda en Temp aunque la pantalla se vuelva a dibujar antes de su primera palabra.
- Los dispositivos ya no se dicen una y otra vez que tienen un archivo, lo que los mantenía ocupados.

### 0.4.019 a 0.4.020

- Una instalación nueva pregunta primero su idioma, y su README y su primera nota se escriben en él. README está en once
  idiomas.
- En el PC, Ctrl+V en Inicio, en una carpeta o en Temp deja allí la imagen o los archivos del portapapeles.
- En el PC, un instalador (MSI): sin derechos de administrador, Mininotes en Configuración, Aplicaciones, y *Desinstalar
  Mininotes…* en el menú, que ofrece una copia de seguridad y te deja conservar tus notas o no. Tras 30 días sin copia
  de seguridad, Mininotes te lo recuerda.
- En el PC, las líneas con negrita, cursiva o enlaces ya no se dibujan unas encima de otras.
- En el teléfono, lo que llega mientras Mininotes está cerrado se reúne en una sola notificación.
- *Sellado en Minima…* lista todo lo sellado, con un + para sellar algo más.

### 0.4.016 a 0.4.018

- El sello de una nota cubre su título además de su texto.
- Temp aparece en Recientes cuando lo usas.
- El texto pegado desde otras apps conserva sus saltos de línea.
- Una imagen puesta en Temp llega antes a tus otros dispositivos.

### 0.4.011 a 0.4.015

- En el teléfono, Ajustes, *El teclado aprende de mis notas*: activado, el dictado por voz del teclado funciona en las
  notas.
- La clave de Integritas va a los dispositivos que elijas. El menú *Esta nota* de una nota tiene *Sellar en Minima…*, un
  archivo sellado queda marcado, y los detalles de un sello enlazan con el Minima Explorer.
- Menos trabajo en cada inicio: los dispositivos ya no se lo repiten todo entre sí.
- El teléfono vuelve a escuchar por sí solo tras reiniciarse o tras una actualización.

### 0.4.010

- *Léeme* ahora es **README**: en Inicio y en GitHub, un mismo texto, este. Un README que nadie ha cambiado se vuelve a
  escribir solo cuando una versión nueva trae uno nuevo; uno en el que escribiste se queda como lo dejaste.

### 0.4.008 a 0.4.009

- Una carpeta de álbum **Fotos** en Inicio por defecto: tus imágenes en una cuadrícula para recorrerlas, y cada una se
  abre en grande con anterior y siguiente.
- Un resultado de búsqueda se abre en la primera coincidencia, con una barra de búsqueda para recorrer todas las
  coincidencias de la nota.
- La conversación del **Agente** usa toda la ventana, con un botón de pantalla completa, y se ofrece a llevarte al lugar
  adecuado, por ejemplo a la parte de Integritas de *Mi perfil*. Guarda la conversación unas horas.
- *Léeme* se muestra u oculta desde *Mostrar en Inicio*, con su propio icono.
- *Léeme* describe el contacto de emergencia (la petición de ayuda) y los límites de los espacios privados.

### 0.4.007

- Una nota sacada de una carpeta compartida en uno de tus dispositivos sigue sincronizándose con tus otros dispositivos.
- Menos idas y vueltas entre dispositivos: la misma respuesta no se envía una y otra vez, así que el teléfono trabaja
  menos.
- En el PC, al dejar el puntero sobre un círculo junto al título de una nota, se ve quién es, qué puede hacer con la
  nota, en qué punto está con lo escrito y cuántos de sus archivos siguen de camino hacia esa persona.

### 0.4.006

- La tarjeta de un PDF muestra la parte de arriba de su primera página; la de un archivo Word, PowerPoint, Excel,
  OpenDocument, RTF, HTML o de texto muestra sus primeras líneas.
- En el teléfono, *Elegir varios…* en un archivo mantenido pulsado marca varios archivos para enviarlos a la vez con
  otra app o ponerlos en la papelera.
- *Mi perfil* recibe ahí mismo la clave API de Integritas, con *Guardar*, y puede pasarla a los dispositivos propios que
  actives. *Sellado en Minima…* pasó al menú.
- En el PC, el tamaño del texto de *Mi perfil* usa la misma escala A− y A+ que el de una nota, y un clic derecho en
  Recientes ofrece *Quitar de Recientes*. El nombre de la app está a la izquierda de la barra, junto al logo.
- Las palabras que se envían con tu enlace están en el idioma que elegiste.
- Las descripciones largas bajo el puntero se muestran enteras, en varias líneas.

### 0.4.005

- Un archivo de una nota puesto en la papelera se quita de la nota en todos los dispositivos y para todas las personas a
  las que llega la nota. *Devolver* lo recupera en todas partes.
- Lo que espera para salir cuenta solo los archivos que muestra la nota, y los enumera uno a uno.
- *Mi perfil* tiene una parte de Integritas, con tu cuenta y la lista de lo que sellaste.
- En el PC, cada entrada del menú tiene una breve descripción que aparece bajo el puntero y se activa o desactiva en *Mi
  perfil*.
- En el teléfono, *Copiar* en un archivo mantenido pulsado pone el propio archivo en el portapapeles; *Enviar con otra
  app…* lleva la marca habitual de compartir.
- Los ajustes de un grupo están tras su ›; *Mi perfil* tiene *Gestionar…* para Personas y dispositivos; cada entrada que
  abre un cuadro o una página termina en "…"; *Guardar como archivo* ahora es *Guardar en mi dispositivo…*.
- La petición de ayuda no deja rastro en ningún menú: solo se activa con un código escrito en una nota privada.
- Escribir en el PC en una nota compartida con mucha actividad ya no va con retraso.

### 0.4.004

- *Personas y dispositivos* y *Mi perfil* (antes *Perfil*) están al final de *Sincronización y compartir*.
- En el PC, *Mi perfil* fija el tamaño del texto de toda la app, en cinco pasos, a partir del siguiente inicio.
- *Guardar en mi ordenador…* (en el teléfono *Guardar en mi teléfono…*) sustituye a *Descargar* y *Guardar una copia…*.
  Todos los cuadros que eligen un archivo o una carpeta en el PC son los propios de Windows.
- La petición de ayuda pasó a estar solo en los espacios privados.

### 0.4.003

- **Sellar en Minima** y **Verificar en Minima** con tu propia cuenta de Integritas.

### 0.4.002

- En el PC, *Copiar* en el menú de un archivo pone una imagen en el portapapeles como imagen, y cualquier archivo como
  archivo.
- En el teléfono, una fila que despliega una elección muestra su valor a la derecha, en una sola línea.

### 0.4.001

- En el PC, el menú del clip marca los archivos de una nota para actuar sobre todos a la vez, y tiene *Hacer una foto* y
  *Grabar audio*.
- *Sincronizar una carpeta de este PC…* vuelve a estar en *Sincronización y compartir*: una carpeta del PC, con sus
  archivos guardados en ella, que se muestra en Mininotes con su propia marca.
- Todos los mensajes que puede mostrar un error están traducidos.

## 2. Instalarlo

**Android** (9 o posterior): descarga `Mininotes-<version>.apk` de [la última
versión](https://github.com/mininotesorg/mininotes/releases/latest), comprueba que es el archivo que se publicó y ábrelo
en el teléfono:

```sh
sha256sum -c Mininotes-<version>.apk.sha256
```

Android pregunta si permites instalar desde donde lo descargaste; ese permiso se puede volver a desactivar después.

**Comprueba quién lo firmó.** Todos los APK publicados están firmados con la misma clave de Mininotes. Esta es la huella
SHA-256 de su certificado, tal como la recibe [AppVerifier](https://github.com/soupslurpr/AppVerifier) (copia las dos
líneas y luego *Verify from clipboard*):

```
org.mininotes.android
F7:91:C2:14:D9:ED:9B:E9:DE:EC:6F:15:BE:D0:25:38:35:22:DD:F9:A8:21:23:4A:73:90:C8:1B:60:1F:91:AB
```

O con el Android SDK: `apksigner verify --print-certs Mininotes-<version>.apk`. Cualquier otra huella no es una
compilación de las versiones publicadas de este repositorio.

**Windows** (10 u 11): descarga `Mininotes-Windows-<version>.msi` de la misma versión y ábrelo. No necesita derechos de
administrador, y Mininotes queda luego en Configuración, Aplicaciones de Windows, como cualquier app. Desinstalarlo allí
conserva tus notas, que una instalación posterior vuelve a encontrar; *Desinstalar Mininotes…* en el menú de la app
también puede eliminarlas, tras ofrecer una copia de seguridad. Java va incluido; no hay que instalar nada más.

O, para quien lo prefiera, `Mininotes-Windows-<version>.zip`: extráelo en cualquier lugar y abre `Mininotes.exe`.

En ninguno de los dos hace falta nada más: el nodo Maxima que lleva tus notas está dentro de la app.

<!-- in the app again -->

## 3. Escribir

- **Una página pautada, lista.** Una nota se abre con el cursor dentro y se guarda mientras escribes. No hay botón de
  guardar.
- **Pestañas.** Una nota puede tener varias páginas, como fichas: *Añadir una pestaña*. Arrastra una pestaña para
  moverla.
- **Negrita, cursiva y subrayado.** Rodea las palabras con las marcas, `**negrita**`, `*cursiva*` y `__subrayado__`, o
  pulsa Ctrl+B, Ctrl+I o Ctrl+U.
- **Dos personas en una nota.** Si dos personas escriben en la misma nota antes de ver lo de la otra, ambos textos se
  juntan línea a línea y no se pierde nada. Lo de cada persona puede mostrarse en su color: toca un círculo bajo el
  título para cambiarlo.
- **Desde otras apps.** En Android, Mininotes está en el menú de compartir: una captura, fotos, archivos, texto o un
  enlace van a una nota nueva o reciente.

## 4. Organizar: Inicio, carpetas, dock y favoritos

- **Inicio** guarda tus notas y carpetas como iconos, en tantas páginas como quieras, cada una donde la pusiste.
- **Crear algo.** Toca + para crear una nota o una carpeta, o para añadir archivos de este dispositivo o de otro.
- **Su menú.** Mantén pulsado un icono para ver su menú: color, icono, mover, compartir y más. En el PC, clic derecho;
  allí el logo lleva a Inicio, y el divisor se arrastra para ensanchar o plegar la lista lateral.
- **Carpetas** guardan notas y otras carpetas, con los niveles que quieras. Arrastra algo sobre una carpeta para meterlo
  dentro.
- **Moverlo.** Mantén pulsado y arrastra un icono: a una carpeta, fuera de ella o a otra página. Al arrastrar, la franja
  de arriba deja soltarlo en un favorito, Temp, Archivados, la papelera o compartir, sin menú.
- **Páginas.** Desliza entre páginas. Pellizca Inicio, o elige *Todas las páginas* en el menú, para verlas todas a la
  vez.
- **Colores e iconos.** Cada nota y carpeta puede tener su color y su icono o imagen, desde su menú. La intensidad del
  color se ajusta con el control bajo los colores. El color de Inicio colorea toda la app.
- **El dock** es la fila al pie de Inicio: lo que pongas ahí, a un toque. *Añadir al dock* está en el menú de cada nota
  y carpeta, salvo en los favoritos. Admite los que quieras y se desplaza de lado si no caben. Cada dispositivo tiene su
  dock, que nunca se envía a los demás.
- **Favoritos** reúne lo que vuelves a abrir. Un favorito nuevo también va al dock; quitar un favorito lo deja en el
  dock.
- **Recientes** muestra lo último que abriste: en el lateral en el PC, en Inicio en el teléfono. *Quitar de Recientes*
  (clic derecho en el PC) lo quita de la lista sin eliminar nada. En el teléfono, desliza hacia arriba desde el dock, o
  toca el asa de encima, para pasar Recientes como tarjetas: toca una para abrirla, deslízala para descartarla o pulsa
  Atrás para cerrar.
- **Temp** es para lo que solo necesitas un tiempo, y para pasar cosas entre tus dispositivos y las personas que elijas:
  lo que hay dentro les llega y desaparece en todas partes al acabar su plazo.
- **Compartido conmigo** guarda lo que otros compartieron contigo, esperando tu respuesta (ver [Compartir con otras
  personas](#6-compartir-con-otras-personas)).
- **Buscar**, al pie de Inicio, encuentra notas, carpetas y archivos por sus palabras. **Árbol** lo muestra todo a la
  vez.
- **Archivados y Papelera**: fuera de la vista, o de salida. De los dos se pueden devolver cosas (ver [Versiones,
  archivados, papelera y copias de seguridad](#11-versiones-archivados-papelera-y-copias-de-seguridad)).
- **README** en Inicio es este texto, el mismo que en GitHub, sin lo que solo importa allí (el contenido, las novedades,
  la instalación, la compilación y la lista de entradas del menú, que el PC explica bajo el puntero).
- **Mostrar en Inicio**, en *Ajustes*, elige qué lugares muestra Inicio (Recientes, Temp, Compartido conmigo,
  Archivados, la papelera) y si el README está ahí. Uno desactivado sigue en el menú.
- **Fotos**: una carpeta de álbum en Inicio por defecto, con sus imágenes en cuadrícula;
  cada una se abre en grande, con la siguiente y la anterior a un gesto o una flecha. Añade fotos con su +, o haz una.
  Cualquier carpeta puede mostrarse como álbum desde su menú.

## 5. Tus propios dispositivos

Vincula tu teléfono y tu PC una vez (*Personas y dispositivos*, luego *Añadir mi dispositivo*, y escanea con uno el
código del otro). Desde entonces, lo que compartes con tus dispositivos se mantiene al día en todos, en los dos
sentidos, unos segundos después de que dejes de escribir. Un dispositivo tuyo puede ponerse en *Puede leer* si solo debe
mirar. Cada uno tiene su disposición: mover una nota en uno no la mueve en el otro, y cada uno tiene su dock.

## 6. Compartir con otras personas

- **Añadir a alguien** escaneando su código o abriendo el enlace que te envía; esa persona hace lo mismo con el tuyo.
- **Compartir una nota, una carpeta o un archivo** desde su menú, *Sincronización y compartir*, luego *Compartir…*.
  Elige primero qué pueden hacer, luego una persona o un grupo, o enseña tu código. Cada persona recibe un rol:
  *Propietario* (quien lo creó), *Administrador*, *Puede escribir* o *Puede leer*, que se cambia en cualquier momento en
  el desplegable de al lado, o se quita.
- **Desde otro dispositivo…**, en cualquier +, recibe lo que alguien te enseña.
- **Grupos**, como Amigos o Colegas, reúnen personas: al compartir con un grupo, todos reciben el mismo rol, también
  quienes entren después.
- **Aceptar o rechazar.** Cuando alguien comparte contigo, una nota te avisa y lo compartido espera en *Compartido
  conmigo*. Ves qué es y quién lo envió, y eliges adónde va antes de que esté en otro sitio. *Aceptar* para quedártelo,
  o *Rechazar*, en silencio o con unas palabras.
- **Los colores viajan.** El color que das a una nota compartida viaja con ella, y cada dispositivo puede fijar su
  propia intensidad.
- **Todo lo que hay en una carpeta compartida se comparte.** Lo que pongas en una carpeta compartida llega a quienes la
  tienen.
- **Los círculos** junto al título de una nota son las personas y dispositivos que la tienen, cada uno con su imagen o
  inicial. Al tocar uno, su imagen se ve en grande. En el PC, el puntero sobre un círculo dice quién es, qué puede hacer
  con la nota, en qué punto está con lo escrito y cuántos archivos le faltan por recibir.

## 7. Personas y dispositivos, y Mi perfil

Los dos están al final de *Sincronización y compartir* en el menú.

- **Personas y dispositivos** muestra las personas que añadiste, tus dispositivos y tus grupos, cada uno en su página.
  La de una persona muestra lo que compartes con ella y te deja ponerle una imagen, que solo ves tú. Los ajustes de un
  grupo están tras su ›.
- **El + junto a Personas** conecta con alguien: escanea su código, pega su enlace o usa *Compartir mi enlace* para
  enviar el tuyo por mensaje o chat. **El + junto a Mis dispositivos** añade otro dispositivo tuyo.
- **Mi perfil** guarda lo que es tuyo:
  - tu **nombre** y tu **imagen**, que llegan a las personas con las que estás vinculado;
  - tu **Dirección de Parlons!**, que se envía sola a tus contactos;
  - tu código y tu enlace, para que otros te añadan;
  - la clave API de **Integritas** (ver [Sellar en Minima con Integritas](#13-sellar-en-minima-con-integritas));
  - en el PC, **Tamaño del texto**: una escala A− y A+ de cinco pasos, de Más pequeño a El más grande, para toda la app
    (Inicio, Recientes, menús, cuadros), aplicada al siguiente inicio, con *Reiniciar ahora*;
  - en el PC, **Describir qué hace cada cosa**, activado por defecto, que activa o desactiva la línea breve que sale con
    el puntero sobre una entrada del menú o un círculo;
  - *Gestionar…*, que abre Personas y dispositivos.

## 8. Saber cómo está todo

- **La marca** de cada nota, carpeta e icono dice cómo está: un anillo gris vacío, solo en este dispositivo; una flecha
  ámbar, esperando para salir (púlsala para enviar ya y ver cada nota y archivo pendiente); tres puntos ámbar, enviado y
  esperando respuesta; una marca verde, todos tienen esta versión; dos barras grises, en pausa; un signo de exclamación
  rojo, un dispositivo sin contacto desde hace más de tres días.
- **Nuevo** muestra lo que cambió desde la última vez: notas en las que escribieron otros, cosas compartidas contigo,
  archivos recibidos, con quién envió cada cosa, cuándo y dónde está.
- **La franja de arriba** aparece cuando algo va mal con un contacto (un dispositivo sin contacto desde hace días,
  bloqueado o con un Mininotes antiguo). Mininotes sigue intentándolo solo, y *Qué hacer* dice lo que puedes hacer.

## 9. Archivos, fotos y grabaciones

- **Adjuntar** cualquier archivo a una nota o una carpeta, varios a la vez.
- **Hacer una foto** desde una nota: la cámara del teléfono, o en el PC la Cámara de Windows, cuyas fotos entran
  directamente en la nota.
- **Grabar audio** en una nota.
- **Ligero para viajar.** Las fotos se reducen al añadirlas, así que una nota con cincuenta fotos viaja ligera; se
  desactiva en *Ajustes*, *Imágenes y archivos*. Los demás archivos se comprimen para ocupar menos.
- **Vistas previas.** La tarjeta de un PDF muestra su primera página; la de un archivo Word, PowerPoint, Excel,
  OpenDocument, RTF, HTML o de texto, sus primeras líneas. Las dibuja Mininotes desde el propio archivo, nunca Windows
  ni otra app, así que no queda ninguna imagen de ellos fuera.
- **Abrir** un archivo en el programa que tu ordenador usa para él (*Abrir en LibreOffice*, por ejemplo), o elegir otro
  con *Abrir con…*. Los cambios que guardes vuelven a Mininotes y van a tus dispositivos.
- **Copiar** en un archivo (clic derecho en el PC, mantener pulsado en el teléfono) pone el archivo en el portapapeles,
  una imagen como imagen, para pegarlo en otra app sin guardarlo antes.
- **Guardarlo fuera.** *Guardar en mi ordenador…* (en el teléfono *Guardar en mi teléfono…*) guarda una copia donde
  elijas. *Guardar en mi dispositivo…* lo conserva también como archivo en una carpeta de este dispositivo.
- **Enviar con otra app…**, en el teléfono, abre el menú de compartir del teléfono, para enviar un archivo por WhatsApp,
  por ejemplo. *Elegir varios…* en un archivo mantenido pulsado marca varios para enviarlos juntos o ponerlos en la
  papelera.
- **Varios a la vez en el PC.** El menú del clip tiene *Adjuntar un archivo*, *Hacer una foto* y *Grabar audio*, y luego
  los archivos de la nota, cada uno con una casilla: *Seleccionar todo*, y luego *Guardar en mi ordenador…* (varios van
  a una sola carpeta), *Sellar en Minima…* o *Mover a la papelera* para los marcados.
- **Enviar archivos** directamente a otro dispositivo tuyo o a alguien que añadiste, sin nota, de puerta a puerta en la
  misma wifi. Lo que llega aparece en Inicio, marcado *nuevo* hasta que lo abres.
- **Sellar en Minima…** (en el menú de un archivo o de una nota): ver [Sellar en Minima con
  Integritas](#13-sellar-en-minima-con-integritas).

## 10. Archivos que se quedan en tu PC

- **Compartir un archivo con Mininotes** (en el PC, en *Sincronización y compartir*): una hoja de cálculo o un documento
  se queda donde está en tu PC y se abre en su propio programa. Cada vez que lo guardas, esa versión va a tus otros
  dispositivos, y una versión guardada en otro sitio se escribe en él. Viaja el archivo entero, la última versión: los
  cambios dentro de una hoja no se combinan. Si cambió en dos sitios antes de sincronizarse, Mininotes pregunta qué
  versión conservar y no reemplaza nada hasta tu respuesta.
- **Sincronizar una carpeta de este PC** (en el PC, en *Sincronización y compartir*): elige una carpeta, en C: o donde
  sea, y todo su contenido aparece en Mininotes como una carpeta marcada como de tu PC. Los archivos que pongas en ella,
  desde el Explorador o cualquier programa, se recogen segundos después y van a tus dispositivos y a quien compartas la
  carpeta; lo que llega de ellos se escribe en ella. Los archivos se quedan en esa carpeta de tu PC, fuera de la app.
  Eliminar uno en el disco nunca lo elimina en Mininotes, ni al revés; si un archivo cambió en los dos sitios, Mininotes
  pregunta qué versión conservar. En un teléfono, los archivos de la carpeta se guardan dentro de la app.
- **Notas como archivos.** Una nota o carpeta también puede guardarse como archivos Markdown en una carpeta del PC que
  elijas (*Guardar en mi dispositivo…*). Mininotes nunca elimina esos archivos, y una nota privada nunca se escribe
  fuera.
- Una carpeta así lleva en su icono una carpeta con flechas. Su menú dice dónde está en el PC, y luego *Mostrar en el
  Explorador*, *Guardar solo dentro* y *Quitar de la app*.

## 11. Versiones, archivados, papelera y copias de seguridad

- **Versiones.** Se guardan todas las versiones de una nota; *Versiones…* devuelve cualquiera.
- **Archivados** guarda cosas fuera de la vista; la **papelera** guarda lo que eliminaste, archivos incluidos, hasta que
  la vacías. Los dos están en Inicio, con una forma de volver a salir. *Devolver* pone una cosa donde estaba.
- **Un archivo de una nota en la papelera.** Un archivo quitado de una nota va a la papelera al instante, sin preguntar,
  y sale de la nota en todos tus dispositivos y para todos a quienes llega la nota. *Devolver* lo recupera en todas
  partes. Solo desaparece para siempre cuando se vacía la papelera.
- **Copias de seguridad.** *Exportar copia de seguridad* escribe un solo archivo con todas las carpetas, notas y
  archivos. Sin el bloqueo (ver [Privacidad, cifrado y seguridad](#14-privacidad-cifrado-y-seguridad)), pide una
  contraseña para esa copia. *Añadir desde copia de seguridad* recupera una y pregunta si añadirla a lo que hay o
  reemplazarlo.

## 12. El Agente: pide, y se hace

El **Agente** (en la barra superior en el PC, junto al menú en el teléfono, o Ctrl+K) responde preguntas sobre Mininotes
con la ayuda que lleva la app ([ayuda en inglés](android/app/src/main/resources/org/mininotes/android/help/en.txt), [en
français](android/app/src/main/resources/org/mininotes/android/help/fr.txt)), y hace tareas sencillas pedidas en inglés
o francés: *what is new?*, *what is on my todo list?*, *add milk to Grocery*, *share Grocery with Carol*, *add a
contact*, *open*, *find*, *new note called…*, *put … in the dock*. Funciona en tu dispositivo sin modelo ni servidor, no
envía nada, enseña antes de hacerlo todo lo que llegue a otros o cambie una nota, y se ofrece a llevarte al lugar
adecuado. Pulsar fuera de su cuadro lo cierra.

## 13. Sellar en Minima con Integritas

Con una cuenta de [Integritas](https://docs.integritas.technology/), un archivo o una nota puede sellarse en la
blockchain de Minima desde su menú. Solo sale del dispositivo su huella (SHA-256), nunca el archivo.

- **Sellar en Minima…** registra ahora que el archivo existe tal como está: una prueba, desde ese momento, de que
  existía y no ha cambiado. Usa un sello de tu cuenta de Integritas.
- **Verificar en Minima…** comprueba un archivo, más tarde, frente a lo registrado: si se selló, si no ha cambiado, y en
  qué bloque y fecha. No cambia nada ni cuesta nada.
- **La diferencia:** sellar escribe la prueba una vez; verificar la lee, tantas veces como quieras.
- **El sello de una nota** cubre su título, una línea vacía y luego su texto; cambiarle el nombre la marca como
  cambiada. Los sellos hechos antes de 0.4.018 cubren solo el texto: *Nota, solo texto*.
- **Mi perfil, Sellado:** pega tu clave API y pulsa *Guardar*; se guarda en tu cuaderno, bloqueada con él, y nunca va en
  una copia de seguridad. Activa cualquiera de tus dispositivos en *También en mis dispositivos* y la clave les llega,
  sellada para cada uno.
- **Sellado en Minima…**, el último en *Buscar*, muestra lo sellado aquí; abre uno para ver su huella, bloque y
  transacción. Su **+** sella una nota o un archivo de Mininotes, o un archivo de este dispositivo que no está en
  Mininotes.

## 14. Privacidad, cifrado y seguridad

- **Sin servidor, sin cuenta, sin rastreo.** Las notas se guardan en tus dispositivos y solo van a las personas y
  dispositivos con quienes las compartes.
- **Sellado de extremo a extremo.** Cada nota y archivo se sella para el dispositivo de destino y lo llevan relés
  [Maxima](https://minima.global), que pasan mensajes que no pueden leer, o va directamente de dispositivo a dispositivo
  en la misma red.
- **Bloquear Mininotes** (*Ajustes*, *Seguridad*) cifra el cuaderno, sus archivos y tus copias de seguridad en el
  dispositivo. Se abre con tu huella o el bloqueo de pantalla en el teléfono y con Windows Hello o tu contraseña en el
  PC, con 12 palabras de recuperación de repuesto: anótalas. Sin el bloqueo, las notas no están cifradas en el
  dispositivo.
- **Dos cosas usan la red:** el nodo Maxima que lleva lo que compartes, y una consulta diaria a este repositorio en
  busca de una versión nueva, que puedes desactivar. [docs/SHARING.md](docs/SHARING.md) explica qué va sellado, qué va
  firmado y qué no está hecho todavía.

## 15. Espacios privados y la petición de ayuda

**Espacios privados**

- **Abrir o crear uno.** En cualquier nota, escribe la palabra private, luego //: y tu contraseña, todo seguido sin nada
  en medio, y pulsa Intro. El código sale de la nota al escribir los dos puntos, y la contraseña va a un cuadro aparte,
  así que nada de ello queda en la nota. El espacio que abre esa contraseña aparece en su propia pantalla, sobre toda la
  ventana. Con una contraseña nueva, escríbela otra vez y se crea un espacio vacío.
- **Un espacio por contraseña.** Cada contraseña distinta es un espacio aparte. No hay límite de cuántos crear, y nada
  en ningún sitio registra cuántos tienes.
- **Anota cada contraseña.** Como nada las registra, una contraseña mal escrita se pide otra vez como espacio nuevo: si
  querías uno existente, cierra el cuadro y vuelve a intentarlo. Un espacio nuevo puede quedar encima de uno anterior y
  hacerle perder una parte.
- **Dónde estás.** La pantalla privada tiene su color y un candado junto a *Privado*, para que siempre sepas dónde
  estás. Crea notas, carpetas y archivos con el + de abajo. El ojo junto a la contraseña la muestra mientras lo
  mantienes pulsado. Atrás cierra el espacio.
- **Eliminar uno.** Ábrelo y elige *Eliminar este espacio privado* en su menú. Desaparece para siempre, y su contraseña
  ya no abre nada.
- **Una contraseña larga.** Usa como contraseña una frase larga de varias palabras. Nadie puede recuperar una olvidada,
  ni siquiera con las 12 palabras de recuperación.
- **Lo que cabe.** Un espacio guarda unos 64 MB en total, suficiente para notas y unas cincuenta fotos. Sus notas,
  carpetas, fotos y archivos no aparecen en otro sitio, por ahora solo están en este dispositivo, no se pueden compartir
  ni enviar, y se cierran al salir de Mininotes o tras cinco minutos sin uso.
- **Un código nunca se guarda en una nota normal.** Si queda uno en una nota, Mininotes lo quita, con el resto de su
  línea, antes de que la nota se guarde, se envíe o se comparta.
- **Archivos.** *Desde este dispositivo…* en el + añade un archivo al espacio, donde se guarda cifrado. El original se
  queda donde estaba salvo que lo elimines, y el selector de archivos del teléfono guarda su propia lista de archivos
  recientes.
- **Consejo.** Puedes tener un espacio creíble con una contraseña y el de verdad con otra, y usar el creíble de vez en
  cuando para que parezca habitado.
- **Sus límites.** Los espacios privados están hechos para que nadie que lea los archivos o el código de la app pueda
  verlos, ni saber cuántos hay. No se puede confirmar que eso sea seguro al 100 %, así que guarda ahí solo lo que no te
  haría daño si se encontrara.

**Contacto de emergencia**

- **Qué es.** Una petición de ayuda envía tu ubicación, y las palabras que elegiste, a las personas que elegiste, en
  cuanto se abre una nota privada. Es para cuando estés en peligro y alguien pueda obligarte a abrir Mininotes.
- **Activar una.** Abre un espacio privado, abre una de sus notas, escribe la palabra help, luego //:, todo seguido, y
  pulsa Intro. El código desaparece al instante y un cuadro pregunta a quién avisar y qué decir. La nota en la que lo
  escribiste es la que la envía. Solo quien conoce la contraseña del espacio puede activarla o quitarla.
- **No se vuelve a mostrar.** Una vez activada, no se vuelve a mostrar nunca, en ningún sitio: ningún menú la menciona,
  y el cuadro siempre se abre vacío.
- **Quitarlas.** Para quitar todas las peticiones de ayuda de un espacio, escribe la palabra erase, luego //:, en una de
  sus notas. Ese código también desaparece al instante.
- **Ubicación.** Mininotes pide para esto el permiso de ubicación del teléfono, y el sistema muestra cuándo se usa.

## 16. Idiomas y traducción

Mininotes habla once idiomas: inglés, francés, alemán, español, italiano, portugués, ruso, chino, japonés, hindi y
persa. Pregunta cuál al primer inicio, con el del dispositivo ya marcado, y este README también está en ese idioma;
*Ajustes, Idioma* cambia los dos. *Traducir…* en el menú de una nota pasa la página a otro idioma en el dispositivo, sin
enviarla a ningún servicio de traducción.

<!-- not in the app -->

## 17. Cada entrada del menú, en una línea

En el PC, al dejar el puntero sobre una entrada del menú se ve lo que hace. *Mi perfil*, *Tamaño del texto*, *Describir
qué hace cada cosa* activa o desactiva estas líneas. Una entrada que termina en "…" abre un cuadro o una página; una sin
ellos actúa al momento.

| Entrada | Qué hace |
|---|---|
| Cambiar nombre | Ponle otro nombre. |
| Quitar esta pestaña | Quita esta pestaña de la nota. |
| Mover a la izquierda | Pon esta pestaña un lugar más a la izquierda. |
| Mover a la derecha | Pon esta pestaña un lugar más a la derecha. |
| Elegir una imagen | Muestra en él una imagen tuya en lugar de su icono. |
| Quitar la imagen | Vuelve a mostrar su icono en lugar de la imagen. |
| Imagen | Elige o quita una imagen tuya para él. |
| Icono | Elige el icono que lleva en Inicio y en las listas. |
| Color | Coloréalo, con la intensidad que elijas. |
| Color de escritura | El color en que se muestra lo que escribe esta persona. |
| Compartiendo | Quién lo tiene y qué puede hacer cada uno. |
| Compartir | Compártelo con personas o tus dispositivos, y elige qué puede hacer cada uno. |
| Ajustes de sincronización | Cuándo y cómo llegan sus cambios a los demás. |
| Sincronizar ahora | Envía ahora lo que espera, y pide a los demás lo suyo. |
| Mostrar mi código | Un código para que otro dispositivo o una persona lo escanee y lo reciba. |
| Compartir un archivo con Mininotes | Sincroniza un archivo de este PC con tus dispositivos. Sigue donde está. |
| Sincronizar una carpeta de este PC | Sincroniza una carpeta entera de este PC con tus dispositivos. Sus archivos siguen en ella. |
| Personas y dispositivos | Las personas y dispositivos con los que compartes, y tus grupos. |
| Mi perfil | Tu nombre, tu código, tu conexión y los ajustes de la app que son tuyos. |
| Desde otro dispositivo | Escanea un código o pega un enlace de otro dispositivo o de una persona. |
| Escanea un código o pega su enlace | Añade a alguien, o uno de tus dispositivos, con su código o enlace. |
| Copiar mi enlace | Copia tu enlace para enviarlo a alguien y que pueda añadirte. |
| Adjuntar un archivo | Guarda un archivo de este PC con esta nota. |
| Añadir un archivo | Guarda un archivo de este PC en esta carpeta. |
| Hacer una foto | Abre la cámara: la foto entra en esta nota. |
| Mostrar como álbum | Muestra sus imágenes en una cuadrícula, para verlas en grande. |
| Añadir fotos | Añade varias imágenes de este dispositivo a la vez. |
| Grabar audio | Graba sonido en esta nota. |
| Adjuntos | Los archivos guardados con esta nota. |
| Mover a | Ponlo en otra carpeta, o en Inicio. |
| Añadir al dock | Ponlo en la fila al pie de Inicio, a un clic. |
| Quitar del dock | Quítalo del dock. No se elimina nada. |
| Añadir a favoritos | Guárdalo entre tus favoritos, y en el dock. |
| Copiar el texto | Copia el texto de la nota para pegarlo en otro lugar. |
| Traducir | Traduce esta página a otro idioma, en este dispositivo. |
| Versiones | Todas las versiones anteriores de esta nota, para verlas o devolverlas. |
| Sellar en Minima | Registra ahora en la blockchain de Minima que existe tal como está. Solo su huella sale de este dispositivo. |
| Verificar en Minima | Comprueba si está en Minima sin cambios, y cuándo se registró. |
| Temporal | Guárdalo solo un tiempo: se va cuando se acaba su plazo. |
| Archivados | Guárdalo aparte, fuera de la vista, conservado. |
| Mover a la papelera | Ponlo en la papelera. Se puede devolver hasta que se vacíe la papelera. |
| Mover a la papelera | Ponlo en la papelera. Se puede devolver hasta que se vacíe la papelera. |
| Nota nueva | Una nota nueva, aquí. |
| Carpeta nueva | Una carpeta nueva, aquí. |
| Carpeta nueva en | Una carpeta nueva dentro de esta. |
| Todas las páginas | Todas las páginas de Inicio a la vez. |
| Volver a la página principal | Volver a la primera página de Inicio. |
| Abrir enlace | Abre este enlace en tu navegador. |
| Copiar enlace | Copia este enlace. |
| Cortar | Corta el texto elegido. |
| Copiar | Cópialo para pegarlo en otro lugar: una imagen como imagen, un archivo como archivo. |
| Pegar | Pega lo que se copió. |
| Seleccionar todo | Selecciona todo. |
| Deshacer | Deshaz el último cambio. |
| Líneas de escritura | Muestra u oculta las líneas de la página. |
| Recientes | Lo que abriste hace poco. |
| Temp | Lo que solo se guarda un tiempo, y va a tus otros dispositivos. |
| Compartido conmigo | Lo que otros compartieron contigo, esperando tu respuesta. |
| Papelera | Lo que se puso en la papelera, para devolverlo o eliminarlo definitivamente. |
| Escritura de recuperación | Lo escrito que no se pudo guardar en su nota, guardado aparte. |
| Exportar copia de seguridad | Escribe un solo archivo con todas las carpetas, notas y archivos. |
| Añadir desde copia de seguridad | Recupera lo que contiene un archivo de copia de seguridad. |
| Compartir Mininotes | Cuéntale a alguien sobre Mininotes. |
| Actualizar a la v | Instala la versión más reciente. |
| Comentarios | Di algo sobre Mininotes, en público, con tu nombre. |
| Ajustes | Cómo funciona Mininotes en este dispositivo. |
| Mininotes en tu móvil | Mininotes en móviles Android, con un código que escanear para conseguirlo. |
| Mininotes en tu PC | Mininotes en Windows, con el enlace para conseguirlo. |
| Acerca de | Qué versión es esta, y quién hizo Mininotes. |
| Desinstalar Mininotes | Quita Mininotes de este PC, conservando tus notas o no, tras ofrecer una copia de seguridad. |
| Salir de Mininotes | Cierra Mininotes. |
| Enviar archivos | Envía archivos directamente a uno de tus dispositivos o a una persona, sin ninguna nota. |
| Archivos enviados | Los archivos enviados desde aquí, y en qué punto está cada uno. |
| Abrir un archivo Markdown | Abre un archivo Markdown de este PC como una nota guardada en ese archivo. |
| Editar un archivo | Abre cualquier archivo de texto de este PC para editarlo, sin guardarlo en Mininotes. |
| Buscar | Encuentra notas, carpetas y archivos por sus palabras. |
| Agente | Haz una pregunta, o pide que se haga algo. |
| Árbol | Todas las carpetas y notas a la vez, en un lateral. |
| Dock | Muestra u oculta el dock en Inicio. |
| Menú | Todo lo que Mininotes puede hacer. |
| Las cosas se quedan | Cuánto tiempo se guarda lo que hay en Temp. |
| Idioma | El idioma de la app. |
| Enviar lo que hay en Temp | Da lo que hay en Temp a tus otros dispositivos. |
| A quién va | Cuáles de tus dispositivos y personas reciben lo que hay en Temp. |
| Mostrar también en Inicio | Muestra también en Inicio lo que hay en Temp. |
| Sincronizar Temp ahora | Envía ahora lo que hay en Temp. |
| Guardar en mi ordenador | Guarda una copia donde elijas en este PC. |
| Guardar en mi teléfono | Guarda una copia donde elijas en este teléfono. |
| Guardar en mi dispositivo | Guárdalo también como archivo en una carpeta de este dispositivo. |
| Quitar | Quítalo. |
| Abrir | Ábrelo. |
| Abrir con | Ábrelo en otro programa. |
| Enviar a un dispositivo | Envíalo directamente a uno de tus dispositivos o a una persona. |
| Desde este dispositivo | Añade archivos de este dispositivo. |
| Qué versión conservar | Cambió en dos lugares: elige qué versión se queda. |
| Mostrar en el Explorador | Muestra dónde está en este PC. |
| Sincronizado con el archivo de este PC | Sincronízalo con su archivo en este PC, o deja de hacerlo. |
| Reemplazar por otro archivo | Pon otro archivo en su lugar. |
| Meter en una nota | Guárdalo con una nota. |
| Quitar de Mininotes | Sácalo de Mininotes. El archivo en este PC se queda. |
| Vaciar la papelera | Elimina definitivamente todo lo que hay en la papelera. |
| Ocultar de Inicio | No lo muestres en Inicio. Sigue en el menú. |
| Esperando | Lo que espera para salir, y por qué. |
| Quitar de Recientes | Quítalo de la lista de lo que abriste hace poco. |
| Devolver | Devuélvelo a donde estaba. |
| Eliminar definitivamente | Elimínalo definitivamente. No se podrá devolver. |
| Tamaño del texto | El tamaño con que se muestra el texto. |
| Igual que las otras notas | Muestra esta nota al tamaño habitual. |
| Encontrarlo | Dile a Mininotes dónde está ahora su archivo. |
| Guardar solo dentro | Deja de guardarlo como archivo en este dispositivo. Sigue en Mininotes. |
| Quitar de la app | Sácalo de Mininotes. Su archivo en este dispositivo se queda. |
| Eliminar grupo | Elimina este grupo. Las personas que lo forman se quedan. |
| Mi dispositivo | Cuéntalo como uno de tus propios dispositivos. |
| Olvidar | Quita este dispositivo de tu lista. No se deja de compartir nada. |
| Quitar a esta persona | Saca a esta persona de todo lo que compartiste con ella, y de tus personas. |
| Cambiar contraseña | Una contraseña nueva para tu cuaderno. |
| Eliminar | Elimínalo. |
| Esta nota | Lo que se puede hacer con la nota abierta. |

<!-- in the app again -->

## 18. Actualizaciones

La app busca una vez al día una versión más reciente y lo avisa en una línea; *Actualizar* la instala. La [última
versión](https://github.com/mininotesorg/mininotes/releases/latest) siempre tiene los archivos.

## 19. Di algo sobre Mininotes

*Comentarios* en el menú de la app abre aquí un formulario ya rellenado, con tu nombre; no se publica nada por ti. Todo
lo que se ha dicho está en [los issues de
comentarios](https://github.com/mininotesorg/mininotes/issues?q=is%3Aissue+label%3Afeedback). Un problema de
**seguridad** va en privado a [un aviso de
seguridad](https://github.com/mininotesorg/mininotes/security/advisories/new), no a un issue.
[docs/FEEDBACK.md](docs/FEEDBACK.md) tiene el resto.

<!-- not in the app -->

## 20. Compílalo tú mismo

**Android** necesita JDK 17 y el Android SDK (compileSdk 36):

```sh
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

El APK queda en `android/app/build/outputs/apk/release/`, sin firmar: fírmalo con tu propia clave, o usa `./gradlew
installDebug`. `android/local.properties` apunta a tu SDK y no está en el repositorio.

**Windows** necesita JDK 17: ejecuta `./windows/build.ps1 -Package` y abre el `Mininotes.exe` de
`dist/latest/Mininotes-Windows-<version>/`. Ver [windows/README.md](windows/README.md).

**Comprobaciones:** unas 870 pruebas unitarias en Android y 1.360 en Windows, sin necesidad de dispositivo. Cubren los
puntos donde un error haría perder lo que alguien escribió: la combinación, las notas que llegan, quién puede hacer qué,
las migraciones del esquema, la vinculación, los adjuntos. Lo que necesita un dispositivo real se comprueba en dos
teléfonos y un PC antes de cada versión, en un registro que el mantenedor guarda en privado, porque nombra sus
dispositivos y cita sus notas. Pide en un issue la entrada de una compilación.

## 21. Qué hay en este repositorio

| Ruta | Qué contiene |
| --- | --- |
| [android/](android/) | La app de Android, y el código que comparten las dos apps. Sin layouts XML: las vistas se construyen en Java. |
| [windows/](windows/README.md) | La app de Windows y su empaquetado. |
| [docs/HOME.md](docs/HOME.md) | Cómo está organizada la app, y cada decisión tomada, numerada. |
| [docs/SHARING.md](docs/SHARING.md) | El diseño de la sincronización: claves, vinculación, combinación, conflictos. |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Para qué sirve la app, y para qué no. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Cómo ayudar, y por dónde empezar. |

Pocas dependencias de terceros en tiempo de ejecución: ZXing para los códigos QR, SQLCipher con AndroidX SQLite para el
cuaderno cifrado, FlatLaf para el aspecto de Windows, PDFBox para la primera página de un PDF en el PC. Está hecho así
para que se pueda leer entero.

<!-- in the app again -->

## 22. Licencia

Mininotes es **software libre y de código abierto** bajo la [GNU General Public License, versión 3 o
posterior](LICENSE). Puedes usarlo, estudiarlo, cambiarlo, distribuirlo y también venderlo, pero lo que distribuyas,
cambiado o no, va con su código fuente bajo la misma licencia. Los términos de terceros están en [NOTICE](NOTICE).

El transporte Maxima que lleva la app es el núcleo de [eurobuddha/maxima](https://github.com/eurobuddha/maxima),
incluido sin modificar con permiso de su autor y acreditado en NOTICE. Es suyo, y no está bajo la GPL; su propio
repositorio todavía no tiene licencia.

<!-- not in the app -->

Hasta el 26 de septiembre de 2026, Mininotes era de código disponible (Apache 2.0 con la Commons Clause y una condición
de producto de pago); las copias obtenidas antes conservan esas condiciones.

Contribuciones: [CONTRIBUTING.md](CONTRIBUTING.md). Informes de seguridad: [SECURITY.md](SECURITY.md).
