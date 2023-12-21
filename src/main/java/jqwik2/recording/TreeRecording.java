package jqwik2.recording;

import java.util.stream.*;

public record TreeRecording(Recording head, Recording child) implements Recording {

	@Override
	public Stream<? extends Recording> shrink() {
		return new TreeShrinker(this).shrink();
	}

	@Override
	public int compareTo(Recording other) {
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
	public boolean isomorphicTo(Recording other) {
		if (other instanceof TreeRecording otherTree) {
			return head.isomorphicTo(otherTree.head) && child.isomorphicTo(otherTree.child);
		}
		return false;
	}

	@Override
	public String toString() {
		return "tree{%s, %s}".formatted(head, child);
	}

}
