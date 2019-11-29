# Gothello Daemon Java Client Library
Bart Massey

The Java client "library" for the Gothello daemon
[`gthd`](http://github.com/pdx-cs-ai/gothello-gthd) is a
standard Java class.  You will need the files
`GthClient.class` and `Move.class`, which you should be able
to get via `javac *.java` with any recent Java compiler.

In brief, this library is used by creating a new `GthClient` object,
which automatically connects to the specified server on the
specified host as the specified player.  (See the
documentation on running a Gothello game for details.)  The
client object then handles the details of making moves and
getting moves from the server, using the `make_move()` and
`get_move()` methods.  These accept and return `Move` objects.
Time control tracking is performed by the client object,
which caches a bunch of state information about the game
in progress in public fields.

There is javadoc [documentation](https://pdx-cs-ai.github.io/gothello-libclient-java/javadoc/index.html) in the
`javadoc` subdirectory of this directory, which should make
the details of using `GthClient` and `Move` clearer.
Also, the
[Grossthello](http://github.com/pdx-cs-ai/gothello-grossthello)
Java Gothello player is available as an example of what a
Java client might look like.

Let me know if there are questions or bugs.
