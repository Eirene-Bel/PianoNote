# PianoNote

[日本語](../../README.md) · [English](README.en.md) · [简体中文](README.zh-Hans.md) · [繁體中文](README.zh-Hant.md) · [한국어](README.ko.md) · [Español](README.es.md) · [Français](README.fr.md) · [Deutsch](README.de.md) · [Português](README.pt.md)

Um aplicativo para registrar a prática de piano, ler partituras, treinar o ouvido e acompanhar a taxa de acertos e o tempo de prática.

- [APK Android](https://github.com/Eirene-Bel/PianoNote/releases/latest) — Android 16 ou posterior
- [Site](https://eirene-bel.github.io/PianoNote/)
- [Experimentar a demonstração](https://eirene-bel.github.io/PianoNote/demo/)

## Recursos

- Registre o conteúdo e a duração da prática, com reflexões opcionais.
- Perguntas aleatórias em clave de sol e de fá: leitura e identificação auditiva de 1–3 notas.
- Cronometragem automática somente enquanto responde; nomes e resultados exibidos acima das notas.
- Taxa de acertos recente, calendário de prática e backups JSON.

Os registros do Android são armazenados no dispositivo. O site público oferece uma apresentação e uma demonstração; os dados inseridos na demonstração não são salvos.

## Idiomas da interface

O aplicativo Android, o site e a demonstração oferecem os nove idiomas listados acima.

Por padrão, é usado o idioma do dispositivo ou navegador. Você pode alterá-lo nas configurações do aplicativo Android ou no seletor de idioma do site. Para idiomas não disponíveis, é usado o inglês.

As traduções estão incluídas e funcionam offline, sem API de tradução ou custos adicionais. Os títulos das peças e as reflexões permanecem como foram escritos. Mudar o idioma não altera a avaliação, os registros salvos nem o formato dos backups JSON.

As notas aparecem como ド–シ em japonês, C–B em inglês, C–H em alemão, 도–시 em coreano e Do–Si adaptado aos demais idiomas. Em inglês e alemão, FA representa duas notas, F + A; nos idiomas que usam solfejo, Fa representa uma única nota.

## Desenvolvimento

| Diretório | Conteúdo |
|---|---|
| `android/` | Aplicativo Android |
| `site/` | Site e demonstração |
| `tests/ · contracts/` | Testes web e dados de teste compartilhados |
| `tools/` | Scripts de compilação e distribuição |

[Instruções de compilação (japonês)](../BUILD.md) · [Avisos de terceiros](../../THIRD_PARTY_NOTICES.md)

## Estado da publicação

Esta documentação descreve o branch deste PR. O site público e o APK mais recente podem ainda não incluir estas alterações.
