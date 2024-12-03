package carpet.helpers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

public class ShitpostSnooper {
    public static class SequencedNumberGenerator extends Random {
        private final BufferedInputStream input;
        private final Random random = new Random();

        public SequencedNumberGenerator(InputStream input) {
            this.input = new BufferedInputStream(input);
            input.mark(Integer.MAX_VALUE);
        }

        public SequencedNumberGenerator(Path path) throws IOException {
            this.input = new BufferedInputStream(Files.newInputStream(path));
            input.mark(Integer.MAX_VALUE);
        }

        private int nextByte() {
            try {
                int i;
                if ((i = input.read()) != -1) return i;
                input.reset();
                if ((i = input.read()) != -1) return i;
            } catch (Throwable ignore) {
            }
            return random.nextInt(256);
        }

        @Override
        protected int next(int bits) {
            // advances some bits
            int result = 0;
            for (int i = 0; i < bits; i += 8) {
                result |= nextByte();
            }
            return result & ((1 << bits) - 1);
        }
    }




}
