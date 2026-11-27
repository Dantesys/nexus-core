# Nexus Core

Mod de Minecraft (NeoForge) de magia e progressão baseado em **mana**, **relíquias únicas** e **disputas entre jogadores**.

O Nexus Core é a base jogável do projeto Nexus. Os elementos (dimensões, mobs e magias elementais) ficam para addons separados, que se conectam ao core por uma API própria.

![Minecraft](https://img.shields.io/badge/Minecraft-26.3-green)
![NeoForge](https://img.shields.io/badge/NeoForge-26.3.0.102-orange)
![Java](https://img.shields.io/badge/Java-25-blue)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

---

## Destaques

- **Mana não elemental com 4 status**, cada um com seu próprio rank (F a S), para montar builds no estilo RPG.
- **14 relíquias únicas por mundo**, cada uma com 5 habilidades que evoluem pela maestria do jogador.
- **Expansão de Domínio** e **Duelos Nexus**: formas de tomar relíquias de outros jogadores.
- **Matrizes**: módulos instalados nas relíquias no Altar de Manipulação / Altar de Ascensão.
- Quase sem dependências externas.

---

## Sistema de mana

Todo jogador desbloqueia a mana não elemental na **Bola de Cristal**. Ela regenera sozinha e também pode vir do **Coletor**.

| Status | O que controla |
| --- | --- |
| Capacidade | Mana máxima |
| Regeneração | Velocidade de recuperação |
| Afinidade | Força das habilidades |
| Ressonância | Duração e redução de cooldown |

- Os status sobem com **núcleos de mana** aplicados na Bola de Cristal. A chance de sucesso depende do encantamento **Distúrbio de Mana** (50% no nível 1, 100% no nível 10).
- Os status formam pares que dividem um limite: **Capacidade × Regeneração** e **Afinidade × Ressonância**. Investir num lado limita o outro.
- O **Núcleo de Anti-mana** reduz um status de forma garantida, para refazer a build.

## Máquinas

| Bloco | Função |
| --- | --- |
| Bola de Cristal | Desbloqueia a mana, mostra o HUD e evolui os status |
| Infusor | Receitas que consomem mana (aço, núcleos, encantamentos) |
| Coletor | Gera mana e entrega ao dono ou a uma máquina vinculada |
| Forja | Produção eficiente de aço e equipamentos |
| Altar de Manipulação / Ascensão | Instala e remove Matrizes nas relíquias |

---

## Relíquias

Cada relíquia é **única no mundo**, fica presa ao inventário do portador e tem **5 habilidades acumuladas** (níveis 1, 5, 10, 15 e 20). A **maestria fica no jogador**: quem toma uma relíquia de outra pessoa começa do zero com ela.

| Relíquia | Tipo | Identidade |
| --- | --- | --- |
| **Rupty** | Espada (katana) | Marcas de corte que rompem a defesa do alvo até a Marca Mortal |
| **Krefty** | Martelo | Construção com mana, áreas de obra e cópia de estruturas |
| **Orix** | Picareta | Mineração: gerar, encontrar e colher minérios |
| **Brekry** | Picareta | Escavação: túneis, dinamite e limpeza de área |
| **Bastry** | Escudo | Guarda o dano bloqueado e reflete projéteis |
| **Skopy** | Arco | Rifle de precisão com flechas de mana |
| **Escordy** | Besta | Espingarda com cargas acumuladas |
| **Agrix** | Enxada | Agricultura e criação de animais |
| **Fisgy** | Vara de pesca | Pesca em água e lava, arpão e muito humor |
| **Brutix** | Machado | Fúria que cresce ao apanhar |
| **Dwarky** | Clava | Anão: golpes de queda, mangual e Home Run |
| **Oculty** | Óculos | Espionagem, invisibilidade e modo espectador |
| **Domix** | Domador | Pets, alcateia e poderes emprestados dos bichos |
| **Rifty** | Lança | Espaço e dimensão: teleporte, subespaço e buraco negro |

### Matrizes

Relíquias não aceitam encantamentos do vanilla. Em vez disso, recebem **Matrizes** no altar:

- Cada Matriz tem um **peso**, e a maestria define quanto peso a relíquia suporta.
- A **Matriz de final** libera a habilidade de nível 20 e só pode ser instalada com maestria máxima.
- As Matrizes ficam na relíquia. Se ela for roubada, o novo dono só ativa cada Matriz quando a maestria dele liberar o peso.

---

## PvP: Domínios e Duelos

- **Expansão de Domínio**: magia de quem **não tem relíquia**. Prende portadores numa arena e, se vencer, toma uma relíquia. Tem cinco modos: Provação, Derrota, Expulsão, Núcleo e Sobrecarga.
- **Duelo Nexus**: desafio formal entre portadores, com aposta de relíquias, moedas de desafio e de recusa, e o **Estado de Zona** depois de cada duelo.
- **Pele de Mana**: magia básica de defesa contra efeitos de domínio e de magia.

Quase todas as regras de PvP são **configuráveis** pelo servidor.

---

## Requisitos

- Minecraft **26.3**
- NeoForge **26.3.0.102** ou mais recente
- Java **25**

## Instalação

1. Instale o NeoForge para a versão 26.3.
2. Baixe o `.jar` do Nexus Core na aba **Releases**.
3. Coloque o arquivo na pasta `mods` do jogo ou do servidor.

## Compilando o código

```bash
git clone https://github.com/Dantesys/nexus.git
cd nexus
./gradlew build
```

O `.jar` gerado fica em `build/libs/`.

Para abrir o jogo em modo de desenvolvimento:

```bash
./gradlew runClient
```

---

## Status

O projeto está em desenvolvimento ativo. As 14 relíquias já estão implementadas; os sistemas de domínio, duelo e Matrizes seguem em construção e balanceamento.

## Histórico

O Nexus começou como o plugin PaperMC **[Nexus Relics](https://github.com/Dantesys/nexus-plugin)** e evoluiu para um mod completo.

## Autor

Feito por **[Dantesys](https://github.com/Dantesys)**.
