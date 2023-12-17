package jqwik2gen;

import java.util.stream.*;

public class TreeShrinker {
	private final SourceRecording head;
	private final SourceRecording child;

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
		Stream<? extends SourceRecording> childCandidates = child.shrink();
		return childCandidates.map(this::replaceChild);
	}

	private TreeRecording replaceChild(SourceRecording childRecording) {
		return new TreeRecording(head, childRecording);
	}

	private Stream<TreeRecording> shrinkHead() {
		Stream<? extends SourceRecording> headCandidates = head.shrink();
		return headCandidates.map(this::replaceHead);
	}

	private TreeRecording replaceHead(SourceRecording headRecording) {
		return new TreeRecording(headRecording, child);
	}
}
