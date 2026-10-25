<!-- Translated from README.md, SHA-256 3aac5412de459a31bef1e0a93eccaf1c557c9be61015e858fa8724583e1f5000 -->

# Mininotes

O Mininotes é um caderno para o seu celular e o seu PC que mantém suas notas, pastas e arquivos iguais em todos os seus
dispositivos e os compartilha com as pessoas que você escolher. Não há servidor, nem conta, nem ninguém no meio: tudo
viaja lacrado de ponta a ponta pela rede Minima, ou direto de dispositivo para dispositivo na mesma Wi-Fi.

[![Versão mais recente](https://img.shields.io/github/v/release/mininotesorg/mininotes?label=latest%20build)](https://github.com/mininotesorg/mininotes/releases/latest)

<!-- not in the app -->

[中文](README.zh.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) ·
[English](README.md) · [Italiano](README.it.md) · [日本語](README.ja.md) · [Русский](README.ru.md) ·
[हिन्दी](README.hi.md) · [فارسی](README.fa.md)

## Conteúdo

1. [Novidades](#1-novidades)
2. [Instalar](#2-instalar)
3. [Escrever](#3-escrever)
4. [Organizar: Início, pastas, barra e favoritos](#4-organizar-início-pastas-barra-e-favoritos)
5. [Seus próprios dispositivos](#5-seus-próprios-dispositivos)
6. [Compartilhar com outras pessoas](#6-compartilhar-com-outras-pessoas)
7. [Pessoas e dispositivos, e Meu perfil](#7-pessoas-e-dispositivos-e-meu-perfil)
8. [Saber como está tudo](#8-saber-como-está-tudo)
9. [Arquivos, fotos e gravações](#9-arquivos-fotos-e-gravações)
10. [Arquivos que ficam no seu PC](#10-arquivos-que-ficam-no-seu-pc)
11. [Versões, arquivados, lixeira e backups](#11-versões-arquivados-lixeira-e-backups)
12. [O Agente: peça e ele faz](#12-o-agente-peça-e-ele-faz)
13. [Carimbar no Minima com Integritas](#13-carimbar-no-minima-com-integritas)
14. [Privacidade, criptografia e segurança](#14-privacidade-criptografia-e-segurança)
15. [Espaços privados e o pedido de ajuda](#15-espaços-privados-e-o-pedido-de-ajuda)
16. [Idiomas e tradução](#16-idiomas-e-tradução)
17. [Cada item do menu, em uma linha](#17-cada-item-do-menu-em-uma-linha)
18. [Atualizações](#18-atualizações)
19. [Diga algo sobre o Mininotes](#19-diga-algo-sobre-o-mininotes)
20. [Compile você mesmo](#20-compile-você-mesmo)
21. [O que há neste repositório](#21-o-que-há-neste-repositório)
22. [Licença](#22-licença)

---

## 1. Novidades

Mais recentes primeiro. Cada versão é igual no celular e no PC, a não ser que se diga o contrário.

### 0.4.024 a 0.4.027

- **Carimbar no Minima…** é uma só linha no menu de uma nota ou de um arquivo, que diz em que ponto está o carimbo, e
  abre uma só janela com tudo: carimbar, procurá-lo no Minima, verificar de novo, as versões carimbadas antes e comparar
  um arquivo com uma impressão digital. O relatório PDF do Integritas pode ser anexado à nota ou guardado ao lado do
  arquivo.
- Os carimbos vão para os seus outros dispositivos: o que foi carimbado no celular aparece na lista do PC, e vice-versa.
- Um arquivo carimbado leva um pequeno carimbo no ícone.
- *Comparar com uma impressão digital…*: cole o SHA-256 que você recebeu para um arquivo e veja na hora se é o mesmo
  arquivo. Nada sai do dispositivo e nenhuma conta é necessária.
- O que o Integritas responde é dito no seu idioma, e quando ele não responde de imediato, a pergunta é feita de novo.
- No PC, as palavras de cada tela, botão e descrição sob o ponteiro estão no seu idioma; as primeiras pastas de um
  caderno novo também recebem nomes nele. Em francês, uma nota nova leva *nouv.*
- O celular não para mais de responder quando a tela é redesenhada enquanto ele ainda tenta alcançar os retransmissores.

### 0.4.021 a 0.4.023

- As abas de uma nota mudam de ordem arrastando-as no PC, ou segurando-as e levando-as no celular; o menu delas também
  tem *Mover para a esquerda* e *Mover para a direita*.
- O idioma é perguntado a cada início até que um seja confirmado; no celular, basta tocar em um idioma.
- Cada app apresenta o outro: *Mininotes no seu celular…* no PC, *Mininotes no seu PC…* no celular, com um código para
  escanear.
- O instalador do Windows mostra o logo do Mininotes e pode abrir o Mininotes ao terminar.
- Uma nota começada em Temp fica em Temp mesmo que a tela seja redesenhada antes da primeira palavra.
- Os dispositivos não dizem mais uns aos outros, de novo e de novo, que têm um arquivo, o que os mantinha ocupados.

### 0.4.019 a 0.4.020

- Uma instalação nova pergunta primeiro o idioma, e o README e a primeira nota são escritos nele. O README está em onze
  idiomas.
- No PC, Ctrl+V no Início, numa pasta ou em Temp coloca ali a imagem ou os arquivos da área de transferência.
- No PC, um instalador (MSI): sem direitos de administrador, o Mininotes em Configurações, Aplicativos, e *Desinstalar o
  Mininotes…* no menu, que oferece um backup e deixa você manter suas notas ou não. Depois de 30 dias sem backup, o
  Mininotes lembra você.
- No PC, linhas com negrito, itálico ou links não são mais desenhadas umas sobre as outras.
- No celular, o que chega enquanto o Mininotes está fechado é reunido numa só notificação.
- *Carimbado no Minima…* lista tudo o que foi carimbado, com um + para carimbar algo mais.

### 0.4.016 a 0.4.018

- O carimbo de uma nota cobre o título além do texto.
- Temp aparece em Recentes quando você o usa.
- Texto colado de outros apps mantém as quebras de linha.
- Uma imagem colocada em Temp chega antes aos seus outros dispositivos.

### 0.4.011 a 0.4.015

- No celular, Configurações, *O teclado aprende com as minhas notas*: ativado, a digitação por voz do teclado funciona
  nas notas.
- A chave do Integritas vai para os dispositivos que você escolher. O menu *Esta nota* de uma nota tem *Carimbar no
  Minima…*, um arquivo carimbado fica marcado, e os detalhes de um carimbo levam ao Minima Explorer.
- Menos trabalho a cada início: os dispositivos não repetem mais tudo uns para os outros.
- O celular volta a escutar sozinho depois de reiniciar ou depois de uma atualização.

### 0.4.010

- *Leia-me* agora é **README**: no Início e no GitHub, um só texto, este. Um README que ninguém alterou é reescrito
  sozinho quando uma versão nova traz um novo; um em que você escreveu fica como você deixou.

### 0.4.008 a 0.4.009

- Uma pasta de álbum **Fotos** no Início por padrão: suas imagens numa grade para percorrer, cada uma aberta em tamanho
  grande com anterior e próxima.
- Um resultado de busca abre na primeira ocorrência, com uma barra de busca para percorrer todas as ocorrências na nota.
- A conversa do **Agente** usa a janela inteira, com um botão de tela cheia, e oferece levar você ao lugar certo, por
  exemplo à parte de Integritas de *Meu perfil*. Ele guarda a conversa por algumas horas.
- *Leia-me* é mostrado ou ocultado em *Mostrar no Início*, com ícone próprio.
- *Leia-me* descreve o contato de emergência (o pedido de ajuda) e os limites dos espaços privados.

### 0.4.007

- Uma nota tirada de uma pasta compartilhada em um dos seus dispositivos continua sincronizando com os outros.
- Menos idas e vindas entre dispositivos: a mesma resposta não é enviada de novo e de novo, então o celular trabalha
  menos.
- No PC, deixar o ponteiro sobre um círculo ao lado do título de uma nota mostra quem é, o que pode fazer com a nota, em
  que ponto está com o que foi escrito e quantos arquivos dela ainda estão a caminho.

### 0.4.006

- O cartão de um PDF mostra o topo da primeira página; o de um arquivo Word, PowerPoint, Excel, OpenDocument, RTF, HTML
  ou de texto mostra as primeiras linhas.
- No celular, *Escolher vários…* num arquivo pressionado marca vários arquivos para enviar de uma vez com outro app ou
  pôr na lixeira.
- *Meu perfil* recebe ali mesmo a chave de API do Integritas, com *Salvar*, e pode passá-la aos seus dispositivos que
  você ativar. *Carimbado no Minima…* foi para o menu.
- No PC, o tamanho do texto de *Meu perfil* usa a mesma escala A− e A+ de uma nota, e um clique direito em Recentes
  oferece *Remover de Recentes*. O nome do app fica à esquerda da barra, ao lado do logo.
- As palavras enviadas com o seu link estão no idioma que você escolheu.
- Descrições longas sob o ponteiro aparecem inteiras, em várias linhas.

### 0.4.005

- Um arquivo de uma nota posto na lixeira sai da nota em todos os dispositivos e para todos que recebem a nota.
  *Restaurar* o traz de volta em todo lugar.
- O que espera para sair conta só os arquivos que a nota mostra, e lista cada um.
- *Meu perfil* tem uma parte de Integritas, com a sua conta e a lista do que você carimbou.
- No PC, cada item do menu tem uma breve descrição sob o ponteiro, que se liga ou desliga em *Meu perfil*.
- No celular, *Copiar* num arquivo pressionado põe o próprio arquivo na área de transferência; *Enviar com outro app…*
  tem o símbolo habitual de compartilhar.
- As configurações de um grupo ficam atrás do seu ›; *Meu perfil* tem *Gerenciar…* para Pessoas e dispositivos; todo
  item que abre uma caixa ou uma página termina com "…"; *Manter como arquivo* agora é *Salvar no meu dispositivo…*.
- O pedido de ajuda não deixa rastro em nenhum menu: só é ativado por um código digitado numa nota privada.
- Escrever no PC numa nota compartilhada muito movimentada não fica mais lento.

### 0.4.004

- *Pessoas e dispositivos* e *Meu perfil* (antes *Perfil*) ficam no fim de *Sincronização e compartilhamento*.
- No PC, *Meu perfil* define o tamanho do texto do app inteiro, em cinco passos, a partir da próxima abertura.
- *Salvar no meu computador…* (no celular *Salvar no meu celular…*) substitui *Baixar* e *Salvar uma cópia…*. Toda caixa
  que escolhe um arquivo ou uma pasta no PC é a do próprio Windows.
- O pedido de ajuda passou a ficar só nos espaços privados.

### 0.4.003

- **Carimbar no Minima** e **Verificar no Minima** com a sua própria conta do Integritas.

### 0.4.002

- No PC, *Copiar* no menu de um arquivo põe uma imagem na área de transferência como imagem, e qualquer arquivo como
  arquivo.
- No celular, uma linha que abre uma lista de opções mostra o valor à direita, numa só linha.

### 0.4.001

- No PC, o menu do clipe marca os arquivos de uma nota para agir sobre eles juntos, e tem *Tirar uma foto* e *Gravar
  áudio*.
- *Sincronizar uma pasta deste PC…* voltou para *Sincronização e compartilhamento*: uma pasta do PC, com os arquivos
  guardados nela, mostrada no Mininotes com marca própria.
- Toda mensagem que um erro pode mostrar está traduzida.

## 2. Instalar

**Android** (9 ou mais recente): baixe `Mininotes-<version>.apk` da [versão mais
recente](https://github.com/mininotesorg/mininotes/releases/latest), confira se é o arquivo publicado e abra-o no
celular:

```sh
sha256sum -c Mininotes-<version>.apk.sha256
```

O Android pergunta se você permite instalar a partir de onde baixou; essa permissão pode ser desligada de novo depois.

**Confira quem assinou.** Todo APK publicado é assinado com a mesma chave do Mininotes. Esta é a impressão digital
SHA-256 do certificado, no formato que o [AppVerifier](https://github.com/soupslurpr/AppVerifier) aceita (copie as duas
linhas e depois use *Verify from clipboard*):

```
org.mininotes.android
F7:91:C2:14:D9:ED:9B:E9:DE:EC:6F:15:BE:D0:25:38:35:22:DD:F9:A8:21:23:4A:73:90:C8:1B:60:1F:91:AB
```

Ou com o Android SDK: `apksigner verify --print-certs Mininotes-<version>.apk`. Qualquer outra impressão digital não é
de uma build das versões publicadas deste repositório.

**Windows** (10 ou 11): baixe `Mininotes-Windows-<version>.msi` da mesma versão e abra. Ele não precisa de direitos de
administrador, e o Mininotes fica então em Configurações, Aplicativos do Windows, como qualquer app. Desinstalar por lá
mantém suas notas, que uma instalação posterior encontra de novo; *Desinstalar o Mininotes…* no menu do app também pode
excluí-las, depois de oferecer um backup. O Java vem junto; não é preciso instalar mais nada.

Ou, para quem preferir, `Mininotes-Windows-<version>.zip`: extraia em qualquer lugar e abra `Mininotes.exe`.

Nos dois, não é preciso mais nada: o nó Maxima que leva suas notas está dentro do app.

<!-- in the app again -->

## 3. Escrever

- **Uma página pautada, pronta.** Uma nota abre com o cursor nela e é salva enquanto você escreve. Não há botão de
  salvar.
- **Abas.** Uma nota pode ter várias páginas, como fichas: *Adicionar uma aba*. Arraste uma aba para movê-la.
- **Negrito, itálico e sublinhado.** Cerque as palavras com as marcas, `**negrito**`, `*itálico*` e `__sublinhado__`, ou
  use Ctrl+B, Ctrl+I ou Ctrl+U.
- **Duas pessoas numa nota.** Se duas pessoas escrevem na mesma nota antes de ver o texto da outra, os dois textos são
  juntados linha por linha e nada se perde. O texto de cada pessoa pode aparecer na cor dela: toque num círculo sob o
  título para mudar uma.
- **De outros apps.** No Android, o Mininotes está no menu de compartilhar: uma captura de tela, fotos, arquivos, texto
  ou um link vão para uma nota nova ou recente.

## 4. Organizar: Início, pastas, barra e favoritos

- **Início** guarda suas notas e pastas como ícones, em quantas páginas você quiser, cada uma onde você a pôs.
- **Criar algo.** Toque em + para criar uma nota ou uma pasta, ou para adicionar arquivos deste dispositivo ou de outro.
- **O menu.** Pressione um ícone para abrir o menu dele: cor, ícone, mover, compartilhar e o resto. No PC, clique
  direito; lá o logo leva ao Início, e o divisor pode ser arrastado para alargar ou recolher a lista lateral.
- **Pastas** guardam notas e outras pastas, em quantos níveis você quiser. Arraste algo sobre uma pasta para pôr dentro.
- **Mover.** Pressione e arraste um ícone: para dentro de uma pasta, para fora dela ou para outra página. Enquanto você
  arrasta, a faixa no topo deixa soltar direto num favorito, no Temp, em Arquivados, na lixeira ou em compartilhar, sem
  menu.
- **Páginas.** Deslize entre as páginas. Faça o gesto de pinça no Início, ou escolha *Todas as páginas* no menu, para
  ver todas de uma vez.
- **Cores e ícones.** Cada nota e pasta pode ter sua cor e seu ícone ou imagem, pelo menu. A intensidade da cor se
  ajusta no controle sob as cores. A cor do Início colore o app inteiro.
- **A barra** é a fileira no pé do Início: o que você puser ali fica a um toque. *Adicionar à dock* está no menu de toda
  nota e pasta, menos nos favoritos. Cabem quantos você quiser, e ela rola para o lado quando não cabem na tela. Cada
  dispositivo tem a sua barra, que nunca é enviada aos outros.
- **Favoritos** reúne o que você sempre abre. Um favorito novo também vai para a barra; tirar um favorito o deixa na
  barra.
- **Recentes** mostra o que você abriu por último: na lateral no PC, no Início no celular. *Remover de Recentes* (clique
  direito no PC) tira da lista sem excluir nada. No celular, deslize para cima a partir da barra, ou toque na alça logo
  acima, para passar os Recentes como cartões: toque num cartão para abrir, deslize para descartar ou pressione Voltar
  para fechar.
- **Temp** é para o que você só precisa por um tempo, e para passar coisas entre os seus dispositivos e as pessoas que
  escolher: o que está nele chega a elas e some em todo lugar quando o prazo acaba.
- **Compartilhado comigo** guarda o que outros compartilharam com você, esperando a sua resposta (veja [Compartilhar com
  outras pessoas](#6-compartilhar-com-outras-pessoas)).
- **Buscar**, no pé do Início, encontra notas, pastas e arquivos pelas palavras. **Árvore** mostra tudo de uma vez.
- **Arquivados e Lixeira**: fora da vista, ou de saída. Dos dois dá para restaurar coisas (veja [Versões, arquivados,
  lixeira e backups](#11-versões-arquivados-lixeira-e-backups)).
- **README** no Início é este texto, o mesmo do GitHub, sem o que só importa lá (o conteúdo, as novidades, a instalação,
  a compilação e a lista de itens do menu, que o PC explica sob o ponteiro).
- **Mostrar no Início**, em *Configurações*, escolhe quais lugares o Início mostra (Recentes, Temp, Compartilhado
  comigo, Arquivados, a lixeira) e se o README fica ali. Um desligado continua no menu.
- **Fotos**: uma pasta de álbum no Início por padrão, com as imagens numa grade; cada
  uma abre em tamanho grande, com a próxima e a anterior a um gesto ou uma seta. Adicione fotos com o + dela, ou tire
  uma. Qualquer pasta pode ser mostrada como álbum pelo menu.

## 5. Seus próprios dispositivos

Vincule o celular e o PC uma vez (*Pessoas e dispositivos*, depois *Adicionar meu dispositivo*, e escaneie com um o
código do outro). A partir daí, o que você compartilha com seus dispositivos se mantém atualizado em todos, nos dois
sentidos, alguns segundos depois que você para de escrever. Um dispositivo seu pode ficar em *Pode ler* se só deve
olhar. Cada um tem seu próprio arranjo: mover uma nota num não a move no outro, e cada um tem sua barra.

## 6. Compartilhar com outras pessoas

- **Adicionar alguém** escaneando o código da pessoa ou abrindo o link que ela envia; ela faz o mesmo com o seu.
- **Compartilhar uma nota, uma pasta ou um arquivo** pelo menu, *Sincronização e compartilhamento*, depois
  *Compartilhar…*. Escolha primeiro o que podem fazer, depois uma pessoa ou um grupo, ou mostre o seu código. Cada
  pessoa recebe um papel: *Dono* (quem criou), *Admin*, *Pode escrever* ou *Pode ler*, que se muda a qualquer momento na
  lista ao lado dela, ou se remove.
- **De outro dispositivo…**, em qualquer +, recebe o que alguém mostra a você.
- **Grupos**, como Amigos ou Colegas, reúnem pessoas: ao compartilhar com um grupo, todos recebem o mesmo papel,
  inclusive quem entrar depois.
- **Aceitar ou recusar.** Quando alguém compartilha com você, uma nota avisa e o item espera em *Compartilhado comigo*.
  Você vê o que é e quem enviou, e escolhe para onde vai antes de estar em qualquer outro lugar. *Aceitar* para ficar
  com ele, ou *Recusar*, em silêncio ou com algumas palavras.
- **As cores viajam.** A cor que você dá a uma nota compartilhada vai com ela, e cada dispositivo pode definir a própria
  intensidade.
- **Tudo numa pasta compartilhada é compartilhado.** O que você puser numa pasta compartilhada chega a quem a tem.
- **Os círculos** ao lado do título de uma nota são as pessoas e dispositivos que a têm, cada um com sua imagem ou
  inicial. Um toque num deles mostra a imagem em tamanho grande. No PC, o ponteiro sobre um círculo diz quem é, o que
  pode fazer com a nota, em que ponto está com o que foi escrito e quantos arquivos ainda faltam chegar.

## 7. Pessoas e dispositivos, e Meu perfil

Os dois ficam no fim de *Sincronização e compartilhamento* no menu.

- **Pessoas e dispositivos** lista as pessoas que você adicionou, seus dispositivos e seus grupos, cada um na sua
  página. A página de uma pessoa mostra o que você compartilha com ela e deixa você dar a ela uma imagem à sua escolha,
  que só você vê. As configurações de um grupo ficam atrás do seu ›.
- **O + ao lado de Pessoas** conecta você a alguém: escaneie o código, cole o link ou use *Compartilhar meu link* para
  enviar o seu por mensagem ou chat. **O + ao lado de Meus dispositivos** adiciona outro dispositivo seu.
- **Meu perfil** guarda o que é seu:
  - seu **nome** e sua **imagem**, que vão para as pessoas com quem você está vinculado;
  - seu **Endereço do Parlons!**, enviado sozinho aos seus contatos;
  - seu código e seu link, para outros adicionarem você;
  - a chave de API do **Integritas** (veja [Carimbar no Minima com Integritas](#13-carimbar-no-minima-com-integritas));
  - no PC, **Tamanho do texto**: uma escala A− e A+ de cinco passos, de Menor a O maior, para o app inteiro (Início,
    Recentes, menus, caixas), aplicada na próxima abertura, com *Reiniciar agora*;
  - no PC, **Descrever o que cada coisa faz**, ligado por padrão, que liga ou desliga a linha curta que aparece com o
    ponteiro sobre um item do menu ou um círculo;
  - *Gerenciar…*, que abre Pessoas e dispositivos.

## 8. Saber como está tudo

- **A marca** em cada nota, pasta e ícone diz como está: um anel cinza vazio, só neste dispositivo; uma seta âmbar,
  esperando para sair (toque nela para enviar já e ver cada nota e arquivo pendente); três pontos âmbar, enviado e
  esperando resposta; um visto verde, todos têm esta versão; duas barras cinza, em pausa; um ponto de exclamação
  vermelho, um dispositivo sem contato há mais de três dias.
- **Novo** mostra o que mudou desde a última vez: notas em que outros escreveram, coisas compartilhadas com você,
  arquivos recebidos, com quem enviou cada um, quando e onde está.
- **A faixa no topo** aparece quando algo vai mal com um contato (um dispositivo sem contato há dias, bloqueado ou com
  um Mininotes antigo). O Mininotes continua tentando sozinho, e *O que fazer* diz o que você pode fazer.

## 9. Arquivos, fotos e gravações

- **Anexar** qualquer arquivo a uma nota ou pasta, vários de uma vez.
- **Tirar uma foto** a partir de uma nota: a câmera do celular, ou no PC a Câmera do Windows, cujas fotos entram direto
  na nota.
- **Gravar áudio** numa nota.
- **Leve para viajar.** As fotos são reduzidas ao serem adicionadas, então uma nota com cinquenta fotos viaja leve;
  desligue isso em *Configurações*, *Imagens e arquivos*. Os outros arquivos são compactados para ocupar menos espaço.
- **Prévias.** O cartão de um PDF mostra a primeira página; o de um arquivo Word, PowerPoint, Excel, OpenDocument, RTF,
  HTML ou de texto, as primeiras linhas. Quem as desenha é o Mininotes, a partir do próprio arquivo, nunca o Windows ou
  outro app, então nenhuma imagem deles fica guardada fora.
- **Abrir** um arquivo no programa que o seu computador usa para ele (*Abrir em LibreOffice*, por exemplo), ou escolher
  outro com *Abrir com…*. As alterações que você salvar voltam ao Mininotes e vão para os seus dispositivos.
- **Copiar** num arquivo (clique direito no PC, pressionar no celular) põe o arquivo na área de transferência, uma
  imagem como imagem, para colar em outro app sem salvar antes.
- **Salvar fora.** *Salvar no meu computador…* (no celular *Salvar no meu celular…*) salva uma cópia onde você escolher.
  *Salvar no meu dispositivo…* também o guarda como arquivo numa pasta deste dispositivo.
- **Enviar com outro app…**, no celular, abre o compartilhamento do próprio celular, para enviar um arquivo pelo
  WhatsApp, por exemplo. *Escolher vários…* num arquivo pressionado marca vários para enviar juntos assim ou pôr na
  lixeira.
- **Vários de uma vez no PC.** O menu do clipe tem *Anexar um arquivo*, *Tirar uma foto* e *Gravar áudio*, e depois os
  arquivos da nota, cada um com uma caixa de seleção: *Selecionar tudo*, e então *Salvar no meu computador…* (vários vão
  para uma só pasta), *Carimbar no Minima…* ou *Mover para a lixeira* para os marcados.
- **Enviar arquivos** direto para outro dispositivo seu ou para alguém que você adicionou, sem nota, de porta a porta na
  mesma Wi-Fi. O que chega aparece no Início, marcado *novo* até você abrir.
- **Carimbar no Minima…** (no menu de um arquivo ou de uma nota): veja [Carimbar no Minima com
  Integritas](#13-carimbar-no-minima-com-integritas).

## 10. Arquivos que ficam no seu PC

- **Compartilhar um arquivo pelo Mininotes** (no PC, em *Sincronização e compartilhamento*): uma planilha ou um
  documento fica onde está no seu PC e abre no próprio programa. Cada vez que você o salva, essa versão vai para os seus
  outros dispositivos, e uma versão salva em outro lugar é gravada nele. Viaja o arquivo inteiro, a última versão:
  alterações dentro de uma planilha não são mescladas. Se mudou em dois lugares antes de sincronizar, o Mininotes
  pergunta qual versão manter e não substitui nada até você responder.
- **Sincronizar uma pasta deste PC** (no PC, em *Sincronização e compartilhamento*): escolha uma pasta, no C: ou em
  qualquer lugar, e todo o conteúdo aparece no Mininotes como uma pasta marcada como do seu PC. Os arquivos que você
  puser nela, pelo Explorador ou por qualquer programa, são recolhidos segundos depois e vão para os seus dispositivos e
  para quem você compartilhar a pasta; o que chega deles é gravado nela. Os arquivos ficam nessa pasta do seu PC, fora
  do app. Excluir um no disco nunca o exclui no Mininotes, nem o contrário; se um arquivo mudou nos dois lugares, o
  Mininotes pergunta qual versão manter. Num celular, os arquivos da pasta ficam dentro do app.
- **Notas como arquivos.** Uma nota ou pasta também pode ser guardada como arquivos Markdown numa pasta do PC à sua
  escolha (*Salvar no meu dispositivo…*). O Mininotes nunca exclui esses arquivos, e uma nota privada nunca é gravada
  fora.
- Uma pasta assim mostra no ícone uma pasta com setas. O menu dela diz onde ela está no PC, depois *Mostrar no
  Explorador*, *Manter só no app* e *Remover do app*.

## 11. Versões, arquivados, lixeira e backups

- **Versões.** Toda versão de uma nota é guardada; *Versões…* restaura qualquer uma.
- **Arquivados** guarda coisas fora da vista; a **lixeira** guarda o que você excluiu, arquivos incluídos, até você
  esvaziá-la. Os dois ficam no Início, com um caminho de volta. *Restaurar* põe uma coisa de volta onde estava.
- **Um arquivo de uma nota na lixeira.** Um arquivo tirado de uma nota vai para a lixeira na hora, sem pergunta, e sai
  da nota em todos os seus dispositivos e para todos que recebem a nota. *Restaurar* o traz de volta em todo lugar. Ele
  só some de vez quando a lixeira é esvaziada.
- **Backups.** *Exportar backup* grava um só arquivo com todas as pastas, notas e arquivos. Sem o bloqueio (veja
  [Privacidade, criptografia e segurança](#14-privacidade-criptografia-e-segurança)), ele pede uma senha para esse
  backup. *Adicionar de backup* traz um de volta e pergunta se deve somar ao que existe ou substituir.

## 12. O Agente: peça e ele faz

O **Agente** (na barra superior no PC, ao lado do menu no celular, ou Ctrl+K) responde perguntas sobre o Mininotes com a
ajuda que vem no app ([ajuda em inglês](android/app/src/main/resources/org/mininotes/android/help/en.txt), [en
français](android/app/src/main/resources/org/mininotes/android/help/fr.txt)), e faz tarefas simples pedidas em inglês ou
francês: *what is new?*, *what is on my todo list?*, *add milk to Grocery*, *share Grocery with Carol*, *add a contact*,
*open*, *find*, *new note called…*, *put … in the dock*. Ele roda no seu dispositivo sem modelo nem servidor, não envia
nada, mostra antes tudo o que chegar a outras pessoas ou mudar uma nota, e oferece levar você ao lugar certo. Tocar fora
da caixa dele a fecha.

## 13. Carimbar no Minima com Integritas

Com uma conta do [Integritas](https://docs.integritas.technology/), um arquivo ou uma nota pode ser carimbado na
blockchain Minima pelo menu. Só a impressão digital (SHA-256) sai do dispositivo, nunca o arquivo.

- **Carimbar no Minima…** registra agora que o arquivo existe exatamente como está: uma prova, a partir desse momento,
  de que ele existia e não mudou desde então. Usa um carimbo da sua conta do Integritas.
- **Verificar no Minima…** confere um arquivo, mais tarde, com o que foi registrado: se foi carimbado, se não mudou, e
  em que bloco e data. Não muda nada e não custa nada.
- **A diferença:** carimbar grava a prova uma vez; verificar a lê, quantas vezes você quiser.
- **O carimbo de uma nota** cobre o título, uma linha vazia e depois o texto; renomeá-la a marca como alterada. Carimbos
  feitos antes da 0.4.018 cobrem só o texto: *Nota, só o texto*.
- **Meu perfil, Carimbo:** cole a sua chave de API e toque em *Salvar*; ela fica no seu caderno, bloqueada com ele, e
  nunca vai para um backup. Ative qualquer dispositivo seu em *Também nos meus dispositivos* e a chave chega a eles
  também, lacrada para cada um.
- **Carimbado no Minima…**, o último em *Buscar*, lista o que foi carimbado aqui; abra um para ver a impressão
  digital, o bloco e a transação. O **+** dele carimba uma nota ou um arquivo do Mininotes, ou um arquivo deste
  dispositivo que não está no Mininotes.

## 14. Privacidade, criptografia e segurança

- **Sem servidor, sem conta, sem rastreamento.** As notas ficam nos seus dispositivos e só vão para as pessoas e
  dispositivos com quem você as compartilha.
- **Lacrado de ponta a ponta.** Cada nota e arquivo é lacrado para o dispositivo de destino e levado por retransmissores
  [Maxima](https://minima.global), que repassam mensagens que não conseguem ler, ou vai direto de dispositivo para
  dispositivo na mesma rede.
- **Bloquear o Mininotes** (*Configurações*, *Segurança*) criptografa o caderno, os arquivos dele e os seus backups no
  dispositivo. Ele abre com a sua digital ou o bloqueio de tela no celular e com o Windows Hello ou a sua senha no PC,
  com 12 palavras de recuperação como chave reserva: anote-as. Sem o bloqueio, as notas não ficam criptografadas no
  dispositivo.
- **Duas coisas usam a rede:** o nó Maxima que leva o que você compartilha, e uma consulta diária a este repositório em
  busca de uma versão nova, que você pode desligar. [docs/SHARING.md](docs/SHARING.md) diz o que é lacrado, o que é
  assinado e o que ainda não foi feito.

## 15. Espaços privados e o pedido de ajuda

**Espaços privados**

- **Abrir ou criar um.** Em qualquer nota, digite a palavra private, depois //: e a sua senha, tudo junto sem nada no
  meio, e pressione Enter. O código sai da nota assim que os dois-pontos são digitados, e a senha vai para uma caixa
  própria, então nada disso fica na nota. O espaço que essa senha abre aparece numa tela própria, sobre a janela
  inteira. Com uma senha nunca usada, digite-a de novo e um espaço novo e vazio é criado.
- **Um espaço por senha.** Cada senha diferente é um espaço separado. Não há limite de quantos criar, e nada em lugar
  nenhum registra quantos você tem.
- **Anote cada senha.** Como nada as registra, uma senha digitada errado é pedida de novo como espaço novo: se você
  queria um existente, feche a caixa e tente outra vez. Um espaço novo pode ficar por cima de um anterior e fazê-lo
  perder uma parte.
- **Onde você está.** A tela privada tem cor própria e um cadeado ao lado de *Privado*, para você sempre saber onde
  está. Crie notas, pastas e arquivos com o + embaixo. O olho ao lado da senha a mostra enquanto você o pressiona.
  Voltar fecha o espaço.
- **Excluir um.** Abra-o e escolha *Excluir este espaço privado* no menu. Ele some de vez, e a senha dele não abre mais
  nada.
- **Uma senha longa.** Use como senha uma frase longa de várias palavras. Ninguém consegue recuperar uma esquecida, nem
  com as 12 palavras de recuperação.
- **O que cabe.** Um espaço guarda cerca de 64 MB no total, o bastante para notas e umas cinquenta fotos. As notas,
  pastas, fotos e arquivos dele não aparecem em nenhum outro lugar, por enquanto ficam só neste dispositivo, não podem
  ser compartilhados nem enviados, e fecham quando você sai do Mininotes ou após cinco minutos sem uso.
- **Um código nunca fica numa nota comum.** Se sobrar um numa nota, o Mininotes o tira, com o resto da linha, antes de a
  nota ser salva, enviada ou compartilhada.
- **Arquivos.** *Deste dispositivo…* no + adiciona um arquivo ao espaço, onde fica criptografado. O original fica onde
  estava, a não ser que você o exclua, e o seletor de arquivos do celular guarda a própria lista de arquivos recentes.
- **Dica.** Você pode ter um espaço convincente sob uma senha e o verdadeiro sob outra, e usar o convincente de vez em
  quando para parecer usado.
- **Os limites.** Os espaços privados são feitos para que ninguém que leia os arquivos ou o código do app consiga
  vê-los, nem saber quantos existem. Não dá para confirmar que isso é 100% seguro, então guarde ali só o que não faria
  mal a você se fosse encontrado.

**Contato de emergência**

- **O que é.** Um pedido de ajuda envia a sua localização, e as palavras que você escolheu, às pessoas que você
  escolheu, assim que uma nota privada é aberta. É para quando você estiver em perigo e alguém puder obrigar você a
  abrir o Mininotes.
- **Ativar um.** Abra um espaço privado, abra uma das notas dele, digite a palavra help, depois //:, tudo junto, e
  pressione Enter. O código some na hora e uma caixa pergunta quem avisar e o que dizer. A nota em que você digitou é a
  que o envia. Só quem sabe a senha do espaço pode ativá-lo ou removê-lo.
- **Nunca mais aparece.** Depois de ativado, ele nunca mais aparece, em lugar nenhum: nenhum menu o menciona, e a caixa
  sempre abre vazia.
- **Remover todos.** Para remover todos os pedidos de ajuda de um espaço, digite a palavra erase, depois //:, numa das
  notas dele. Esse código também some na hora.
- **Localização.** O Mininotes pede para isso a permissão de localização do celular, e o sistema mostra quando ela é
  usada.

## 16. Idiomas e tradução

O Mininotes fala onze idiomas: inglês, francês, alemão, espanhol, italiano, português, russo, chinês, japonês, hindi e
persa. Ele pergunta qual na primeira abertura, com o do dispositivo já marcado, e este README também fica nesse idioma;
*Configurações, Idioma* muda os dois. *Traduzir…* no menu de uma nota passa a página para outro idioma no dispositivo,
sem enviá-la a nenhum serviço de tradução.

<!-- not in the app -->

## 17. Cada item do menu, em uma linha

No PC, deixar o ponteiro sobre um item do menu mostra o que ele faz. *Meu perfil*, *Tamanho do texto*, *Descrever o que
cada coisa faz* liga ou desliga essas linhas. Um item que termina com "…" abre uma caixa ou uma página; um sem eles age
na hora.

| Item | O que faz |
|---|---|
| Renomear | Dê outro nome a ele. |
| Remover esta aba | Tire esta aba da nota. |
| Mover para a esquerda | Coloque esta aba uma posição mais à esquerda. |
| Mover para a direita | Coloque esta aba uma posição mais à direita. |
| Escolher uma imagem | Mostre uma imagem sua no lugar do ícone. |
| Remover a imagem | Mostre de novo o ícone no lugar da imagem. |
| Imagem | Escolha ou remova uma imagem sua para ele. |
| Ícone | Escolha o ícone que ele usa no Início e nas listas. |
| Cor | Dê uma cor a ele, na intensidade que escolher. |
| Cor da escrita | A cor em que aparece o que esta pessoa escreve. |
| Compartilhando | Quem tem e o que cada um pode fazer. |
| Compartilhar | Compartilhe com pessoas ou com seus dispositivos, e escolha o que cada um pode fazer. |
| Parâmetros de sincronização | Quando e como suas alterações vão para os outros. |
| Sincronizar agora | Envie agora o que está esperando, e peça aos outros o deles. |
| Mostrar meu código | Um código para outro dispositivo ou uma pessoa escanear e receber. |
| Compartilhar um arquivo pelo Mininotes | Sincronize um arquivo deste PC com seus dispositivos. Ele fica onde está. |
| Sincronizar uma pasta deste PC | Sincronize uma pasta inteira deste PC com seus dispositivos. Os arquivos ficam nela. |
| Pessoas e dispositivos | As pessoas e dispositivos com quem você compartilha, e seus grupos. |
| Meu perfil | Seu nome, seu código, sua conexão e as configurações do app que são suas. |
| De outro dispositivo | Escaneie um código ou cole um link de outro dispositivo ou de uma pessoa. |
| Escanear um código ou colar o link da pessoa | Adicione alguém, ou um dos seus dispositivos, pelo código ou link. |
| Copiar meu link | Copie seu link para enviar a alguém, assim a pessoa pode adicionar você. |
| Anexar um arquivo | Guarde um arquivo deste PC com esta nota. |
| Adicionar um arquivo | Guarde um arquivo deste PC nesta pasta. |
| Tirar uma foto | Abra a câmera: a foto entra nesta nota. |
| Mostrar como álbum | Mostre as imagens numa grade, para vê-las em tamanho grande. |
| Adicionar fotos | Adicione várias imagens deste dispositivo de uma vez. |
| Gravar áudio | Grave som nesta nota. |
| Anexos | Os arquivos guardados com esta nota. |
| Mover para | Coloque em outra pasta, ou no Início. |
| Adicionar à dock | Coloque na fileira no rodapé do Início, a um clique. |
| Remover da barra | Tire da barra. Nada é excluído. |
| Adicionar aos favoritos | Guarde entre seus favoritos, e na barra. |
| Copiar o texto | Copie o texto da nota para colar em outro lugar. |
| Traduzir | Traduza esta página para outro idioma, neste dispositivo. |
| Versões | Todas as versões anteriores desta nota, para ver ou restaurar. |
| Carimbar no Minima | Registre agora na blockchain Minima que ele existe como está. Só a impressão digital sai deste dispositivo. |
| Verificar no Minima | Confira se está no Minima sem alterações, e quando foi registrado. |
| Temporário | Guarde só por um tempo: ele some quando o prazo acaba. |
| Arquivados | Guarde longe da vista, conservado. |
| Mover para a lixeira | Coloque na lixeira. Pode ser restaurado até a lixeira ser esvaziada. |
| Mover para a lixeira | Coloque na lixeira. Pode ser restaurado até a lixeira ser esvaziada. |
| Nova nota | Uma nota nova, aqui. |
| Nova pasta | Uma pasta nova, aqui. |
| Nova pasta em | Uma pasta nova dentro desta. |
| Todas as páginas | Todas as páginas do Início de uma vez. |
| Voltar à página principal | Voltar à primeira página do Início. |
| Abrir link | Abra este link no seu navegador. |
| Copiar link | Copie este link. |
| Recortar | Recorte o texto escolhido. |
| Copiar | Copie para colar em outro lugar: uma imagem como imagem, um arquivo como arquivo. |
| Colar | Cole o que foi copiado. |
| Selecionar tudo | Selecione tudo. |
| Desfazer | Desfaça a última alteração. |
| Linhas de escrita | Mostre ou oculte as linhas da página. |
| Recentes | O que você abriu recentemente. |
| Temp | O que fica só por um tempo, e vai para seus outros dispositivos. |
| Compartilhado comigo | O que outros compartilharam com você, esperando sua resposta. |
| Lixeira | O que foi para a lixeira, para restaurar ou excluir de vez. |
| Escrita de recuperação | O que foi escrito e não pôde ser salvo na nota, guardado à parte. |
| Exportar backup | Grave um único arquivo com todas as pastas, notas e arquivos. |
| Adicionar de backup | Recupere o que um arquivo de backup contém. |
| Compartilhar o Mininotes | Conte a alguém sobre o Mininotes. |
| Atualizar para a v | Instale a versão mais nova. |
| Opinião | Diga algo sobre o Mininotes, publicamente, com seu nome. |
| Configurações | Como o Mininotes funciona neste dispositivo. |
| Mininotes no seu celular | O Mininotes nos celulares Android, com um código para escanear e obtê-lo. |
| Mininotes no seu PC | O Mininotes no Windows, com o link para obtê-lo. |
| Sobre | Qual é esta versão, e quem fez o Mininotes. |
| Desinstalar o Mininotes | Remove o Mininotes deste PC, mantendo ou não suas notas, depois de oferecer um backup. |
| Sair do Mininotes | Feche o Mininotes. |
| Enviar arquivos | Envie arquivos direto para um dos seus dispositivos ou uma pessoa, sem nota. |
| Arquivos enviados | Os arquivos enviados daqui, e em que ponto está cada um. |
| Abrir um arquivo Markdown | Abra um arquivo Markdown deste PC como uma nota guardada nesse arquivo. |
| Editar um arquivo | Abra qualquer arquivo de texto deste PC para editar, sem guardar no Mininotes. |
| Buscar | Encontre notas, pastas e arquivos pelas palavras. |
| Agente | Faça uma pergunta, ou peça que algo seja feito. |
| Árvore | Todas as pastas e notas de uma vez, na lateral. |
| Barra | Mostre ou oculte a barra no Início. |
| Menu | Tudo o que o Mininotes pode fazer. |
| Tempo de permanência | Por quanto tempo o que está no Temp é guardado. |
| Idioma | O idioma do app. |
| Enviar o que está no Temp | Passe o que está no Temp para seus outros dispositivos. |
| Para quem vai | Quais dos seus dispositivos e pessoas recebem o que está no Temp. |
| Mostrar também no Início | Mostre também no Início o que está no Temp. |
| Sincronizar o Temp agora | Envie agora o que está no Temp. |
| Salvar no meu computador | Salve uma cópia onde escolher neste PC. |
| Salvar no meu celular | Salve uma cópia onde escolher neste celular. |
| Salvar no meu dispositivo | Guarde também como arquivo numa pasta deste dispositivo. |
| Remover | Remova. |
| Abrir | Abra. |
| Abrir com | Abra em outro programa. |
| Enviar a um dispositivo | Envie direto para um dos seus dispositivos ou uma pessoa. |
| Deste dispositivo | Adicione arquivos deste dispositivo. |
| Qual versão manter | Mudou em dois lugares: escolha qual versão fica. |
| Mostrar no Explorador | Mostre onde está neste PC. |
| Sincronizado com o arquivo deste PC | Sincronize com o arquivo dele neste PC, ou pare. |
| Substituir por outro arquivo | Coloque outro arquivo no lugar. |
| Colocar numa nota | Guarde com uma nota. |
| Remover do Mininotes | Tire do Mininotes. O arquivo neste PC fica. |
| Esvaziar a lixeira | Exclua de vez tudo o que está na lixeira. |
| Ocultar do Início | Não mostre no Início. Continua no menu. |
| Aguardando | O que está esperando para sair, e por quê. |
| Remover de Recentes | Tire da lista do que você abriu recentemente. |
| Restaurar | Coloque de volta onde estava. |
| Excluir de vez | Exclua de vez. Não poderá ser restaurado. |
| Tamanho do texto | O tamanho em que o texto aparece. |
| Igual às outras notas | Mostre esta nota no tamanho normal. |
| Encontrar | Diga ao Mininotes onde o arquivo está agora. |
| Manter só no app | Pare de guardar como arquivo neste dispositivo. Continua no Mininotes. |
| Remover do app | Tire do Mininotes. O arquivo dele neste dispositivo fica. |
| Excluir grupo | Exclua este grupo. As pessoas dele continuam. |
| Meu dispositivo | Conte como um dos seus próprios dispositivos. |
| Esquecer | Tire este dispositivo da sua lista. Nenhum compartilhamento é desfeito. |
| Remover esta pessoa | Tire esta pessoa de tudo o que você compartilhou com ela, e das suas pessoas. |
| Alterar senha | Uma nova senha para seu caderno. |
| Excluir | Exclua. |
| Esta nota | O que dá para fazer com a nota aberta. |

<!-- in the app again -->

## 18. Atualizações

O app procura uma vez por dia uma versão mais nova e avisa numa linha; *Atualizar* a instala. A [versão mais
recente](https://github.com/mininotesorg/mininotes/releases/latest) sempre tem os arquivos.

## 19. Diga algo sobre o Mininotes

*Opinião* no menu do app abre aqui um formulário já preenchido, com o seu nome; nada é publicado por você. Tudo o que já
foi dito está nas [issues de opinião](https://github.com/mininotesorg/mininotes/issues?q=is%3Aissue+label%3Afeedback).
Um problema de **segurança** vai em particular para [um aviso de
segurança](https://github.com/mininotesorg/mininotes/security/advisories/new), não para uma issue.
[docs/FEEDBACK.md](docs/FEEDBACK.md) tem o resto.

<!-- not in the app -->

## 20. Compile você mesmo

**Android** precisa do JDK 17 e do Android SDK (compileSdk 36):

```sh
cd android
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

O APK fica em `android/app/build/outputs/apk/release/`, sem assinatura: assine com a sua própria chave, ou use
`./gradlew installDebug`. `android/local.properties` aponta para o seu SDK e não está no repositório.

**Windows** precisa do JDK 17: rode `./windows/build.ps1 -Package` e abra o `Mininotes.exe` em
`dist/latest/Mininotes-Windows-<version>/`. Veja [windows/README.md](windows/README.md).

**Verificações:** cerca de 870 testes unitários no Android e 1.360 no Windows, sem precisar de dispositivo. Eles cobrem
os pontos em que um erro faria alguém perder o que escreveu: a mesclagem, as notas que chegam, quem pode fazer o quê, as
migrações de esquema, o pareamento, os anexos. O que precisa de um dispositivo real é verificado em dois celulares e um
PC antes de cada versão, num registro que o mantenedor guarda em particular, porque cita os dispositivos e as notas
dele. Peça numa issue o registro de uma build.

## 21. O que há neste repositório

| Caminho | O que contém |
| --- | --- |
| [android/](android/) | O app Android, e o código que os dois apps compartilham. Sem layouts XML: as telas são montadas em Java. |
| [windows/](windows/README.md) | O app Windows e o empacotamento dele. |
| [docs/HOME.md](docs/HOME.md) | Como o app é organizado, e cada decisão tomada, numerada. |
| [docs/SHARING.md](docs/SHARING.md) | O projeto da sincronização: chaves, pareamento, mesclagem, conflitos. |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Para que serve o app, e para que não serve. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Como ajudar, e por onde começar. |

Poucas dependências de terceiros em tempo de execução: ZXing para os códigos QR, SQLCipher com AndroidX SQLite para o
caderno criptografado, FlatLaf para o visual no Windows, PDFBox para a primeira página de um PDF no PC. Foi feito assim
para que tudo possa ser lido.

<!-- in the app again -->

## 22. Licença

O Mininotes é **software livre e de código aberto** sob a [GNU General Public License, versão 3 ou posterior](LICENSE).
Você pode usá-lo, estudá-lo, alterá-lo, repassá-lo e também vendê-lo, mas o que você repassar, alterado ou não, vai com
o código-fonte sob a mesma licença. Os termos de terceiros estão em [NOTICE](NOTICE).

O transporte Maxima dentro do app é o núcleo de [eurobuddha/maxima](https://github.com/eurobuddha/maxima), incluído sem
modificações com a permissão do autor e creditado no NOTICE. Ele é do autor, e não está sob a GPL; o repositório dele
ainda não tem licença.

<!-- not in the app -->

Até 26 de setembro de 2026, o Mininotes era de código disponível (Apache 2.0 com a Commons Clause e uma condição de
produto pago); as cópias obtidas antes disso mantêm esses termos.

Contribuições: [CONTRIBUTING.md](CONTRIBUTING.md). Relatórios de segurança: [SECURITY.md](SECURITY.md).
