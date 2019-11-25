public class StringChars {
    char chars[];
    int first, last;
    
    public StringChars(String s) {
	first = 0;
	last = s.length();
	chars = new char[last];
	s.getChars(0, last, chars, 0);
    }

    public StringChars(StringBuffer s) {
	first = 0;
	last = s.length();
	chars = new char[last];
	s.getChars(0, last, chars, 0);
    }

    public char charAt(int n) {
	if (first + n >= last)
	    throw new StringIndexOutOfBoundsException();
	return chars[first + n];
    }

    public void deleteFirstChar() {
	if (first >= last)
	    throw new StringIndexOutOfBoundsException();
	first++;
    }

    public void deleteFirstChars(int n) {
	if (first + n > last)
	    throw new StringIndexOutOfBoundsException();
	first += n;
    }

    public int length() {
	return last - first;
    }

    public String substring(int start, int end) {
	if (start > end)
	    throw new IllegalArgumentException();
	if (first + end > last)
	    throw new StringIndexOutOfBoundsException();
	return new String(chars, first + start, end - start);
    }
}
