# Gthd: Gothello daemon
Bart Massey

This is a simple Java daemon for
[Gothello](http://pdx-cs-ai.github.io/gothello-project)
games.  Gthd allows two clients and a number of observers to
connect and play a game. It referees the game and reports
moves to each side and the observers.

Build with `javac Gthd.java`. Run with for example

        java Gthd 0

which starts the server as "server number" 0. Additional
optional arguments can be used to enable time controls. When
run as

        java Gthd 0 120

the server will allow a total of 120 minutes for each side
to make its move. Asymmetric time controls are also
possible: running as

        java Gthd 0 120 60

gives white 120 seconds and black 60 seconds.
