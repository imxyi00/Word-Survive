WORD SURVIVE

Folder contents
  WordSurvive.jar   the game (compiled code)
  src/              all Java source code
  assets/           all images, the font (font.ttf), the music (assets/music), sentences.csv and vocab.csv
  run*.bat / run*.sh / run*.command    launchers      build.bat / build.sh   rebuild the jar from src/
  data/             created on first run (accounts, scores, settings)

Play
  Windows : double-click run.bat        (picks 1920x1080 on a Full HD or bigger screen, otherwise 1280x720)
            run_1080p.bat  = force 1920x1080      run_720p.bat = force 1280x720
  macOS   : double-click run.command  (first time: right-click > Open)   also run_1080p.command / run_720p.command
  Linux   : ./run.sh   ./run_1080p.sh   ./run_720p.sh
  Any OS  : java -jar WordSurvive.jar 1920x1080
Needs Java 17 or newer (https://adoptium.net).

Changing things
  Images, music, font, sentences and vocabulary: just replace or edit the files in assets/ - no rebuild needed.
  Music: assets/music/PandaWindow.wav and Bikes_At_the_Pier.wav (16-bit WAV), keep the names.
  Source code in src/: after editing run build.bat (Windows) or ./build.sh (Mac/Linux). Needs a JDK 17+.

Screen size
  If the window does not fit your screen (1920x1080 on a 1080p monitor) it opens borderless and centered.
  Close it with Ctrl+Q or Alt+F4. Any width works: java -jar WordSurvive.jar 1600x900
