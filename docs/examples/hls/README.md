# MultiRoute HLS example / Тестовый HLS-поток

12 seconds of generated test patterns and a quiet 440 Hz tone, with two H.264/AAC variants (180p and 360p). Unencrypted VOD, continuous timeline, muxed audio. These are media test data, not application source code.

12 секунд синтетической тестовой картинки и тонального звука 440 Гц. Два варианта H.264/AAC: 180p и 360p. Запись без шифрования, непрерывная временная шкала, звук внутри сегментов. Это тестовые медиаданные, а не исходники приложения.

**[Copy the direct HLS URL / Прямая ссылка m3u8](https://raw.githubusercontent.com/2018-spec-fdsds/multiroute-downloader/main/docs/examples/hls/master.m3u8)**

**[Play in the official hls.js demo / Открыть в официальном проигрывателе hls.js](https://hlsjs.video-dev.org/demo/?src=https%3A%2F%2Fraw.githubusercontent.com%2F2018-spec-fdsds%2Fmultiroute-downloader%2Fmain%2Fdocs%2Fexamples%2Fhls%2Fmaster.m3u8)**

On the demo page, enable discovery in the MultiRoute extension and start playback or reload the stream. Opening this GitHub folder does not itself play a video. The external player is maintained by video-dev and is not a MultiRoute service.

На странице проигрывателя включите обнаружение в расширении MultiRoute и запустите воспроизведение либо повторную загрузку потока. Просмотр этой папки GitHub сам по себе не воспроизводит видео. Внешний проигрыватель поддерживает video-dev; это не сервис MultiRoute.

[English walkthrough](../../hls/HLS-CHROME-EDGE.en.md) · [Подробная инструкция](../../hls/HLS-CHROME-EDGE.ru.md)

## License / Лицензия

The project owner dedicates these generated test media files and playlists to the public domain under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). This dedication applies only to the files in this example directory, not to MultiRoute Downloader or its dependencies. Generated with FFmpeg as a development tool; FFmpeg is not included here.

Владелец проекта передаёт эти сгенерированные тестовые медиафайлы и плейлисты в общественное достояние по CC0 1.0. Условия относятся только к файлам этого примера, не к приложению и его зависимостям. Для генерации использован FFmpeg как инструмент разработки; сам FFmpeg здесь не распространяется.
