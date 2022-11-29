# SISA Assmebler in Java

A Java implementation of an assembler for SISA.

Run the jar with `-h` or `--help` to get the command line reference.

## API usage

You can also use the assembler as an API.

The main API entrypoint is the `Assembler` class. The constructor argument is where you want it to write the produced assembly to.
You can instantiate by passing it either a `Path` or an `OutputStream`. Passing it a `Path` will make it write to the file it points
to, creating it if it doesn't exist or truncating it if it does.
If you pass it an `OutputStream`, the assembler will just push the instructions to it, in the form of multiple two-byte arrays (full instructions)
to its `write(byte[])` method.

Note that the `Assembler` is `Closeable`: You should use it in a `try-with-resources` block to ensure it releases the resources it may be holding.
Note that this will close the passed `OutputStream` if this is the way it was instantiated.

Once you've got your `Assembler` instance, the `assemble` methods will make it assemble what you pass it into its output. You can pass it
either a `Path`, in which case it'll assemble the given file, or a `Stream` of `String` instructions.

Here's a basic example for assembling a file into another file:

```java
Path in, out;
try (Assembler assembler = new Assembler(out)) {
	assembler.assemble(in);
} catch (IOException e) {
	// do something
}
```

The `Assembler` will keep track of whether it failed and you can check that by calling its `failed()` method, and the `errors()` method to get the
number of lines that failed to assemble. Note that the output `OutputStream` or file state is undefined if assembly failed, so you probably
should implement some kind of error handling, such as cleaning up the passed file.

### Error handling

By default, errors will be reported to the standard error, `System.err`. However, you can (and are encouraged) to customize error handling to your needs,
by subclassing and overriding the `failedLine` method. The `failedLine` method is called for every line that fails to assemble, with the (trimmed)
line that was being compiled, its line number and the `AssembleException` that caused assembly to fail.

### Assembling to memory

You can simply use Java's `ByteArrayOutputStream` if you want to assemble something to memory instead of to a file.

### Verbose logging with the API

Via the API, the assembler will not output anything (other than, as mentioned, errors in the default `failedLine` implementation). While not supported,
you can toggle the `verbose` (and optionally the `excessivelyVerbose`) fields in the `altrisi.sisaassembler.Logging` class via ways like
reflection in order to receive verbose logging similar to the one the command-line arguments would give you, to the standard output (`System.out`).
