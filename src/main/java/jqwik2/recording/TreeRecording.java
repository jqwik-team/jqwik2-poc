package jqwik2.recording;

import java.util.stream.*;

public record TreeRecording(ChoicesRecording head, ChoicesRecording child) implements ChoicesRecording {

	@Override
	public Stream<? extends ChoicesRecording> shrink() {
		return new TreeShrinker(this).shrink();
	}

	@Override
	public int compareTo(ChoicesRecording other) {
		if (other instanceof TreeRecording otherTree) {
			int headComparison = this.head.compareTo(otherTree.head);
			if (headComparison != 0) {
				return headComparison;
			}
			return this.child.compareTo(otherTree.child);
		}
		return 0;
	}

	@Override
	public String toString() {
		return "tree{%s, %s}".formatted(head, child);
	}

}
