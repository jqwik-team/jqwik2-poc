package jqwik2.recording;

import java.util.stream.*;

class TreeShrinker {
	private final Recording head;
	private final Recording child;

	TreeShrinker(TreeRecording treeRecording) {
		this.head = treeRecording.head();
		this.child = treeRecording.child();
	}

	Stream<TreeRecording> shrink() {
		return Stream.concat(
			shrinkHead(),
			shrinkChild()
		);
	}

	private Stream<TreeRecording> shrinkChild() {
		Stream<? extends Recording> childCandidates = child.shrink();
		return childCandidates.map(this::replaceChild);
	}

	private TreeRecording replaceChild(Recording childRecording) {
		return new TreeRecording(head, childRecording);
	}

	private Stream<TreeRecording> shrinkHead() {
		Stream<? extends Recording> headCandidates = head.shrink();
		return headCandidates.map(this::replaceHead);
	}

	private TreeRecording replaceHead(Recording headRecording) {
		return new TreeRecording(headRecording, child);
	}
}
