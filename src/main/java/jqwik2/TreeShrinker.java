package jqwik2;

import java.util.stream.*;

public class TreeShrinker {
	private final ChoicesRecording head;
	private final ChoicesRecording child;

	public TreeShrinker(TreeRecording treeRecording) {
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
		Stream<? extends ChoicesRecording> childCandidates = child.shrink();
		return childCandidates.map(this::replaceChild);
	}

	private TreeRecording replaceChild(ChoicesRecording childRecording) {
		return new TreeRecording(head, childRecording);
	}

	private Stream<TreeRecording> shrinkHead() {
		Stream<? extends ChoicesRecording> headCandidates = head.shrink();
		return headCandidates.map(this::replaceHead);
	}

	private TreeRecording replaceHead(ChoicesRecording headRecording) {
		return new TreeRecording(headRecording, child);
	}
}
