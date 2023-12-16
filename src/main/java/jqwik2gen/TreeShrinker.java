package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public class TreeShrinker {
	private final SourceRecording head;
	private final List<SourceRecording> children;

	public TreeShrinker(TreeRecording treeRecording) {
		this.head = treeRecording.head();
		this.children = treeRecording.children();
	}

	Stream<TreeRecording> shrink() {
		return Stream.concat(
			shrinkHead(),
			shrinkChildren()
		).sorted();
	}

	private Stream<TreeRecording> shrinkChildren() {
		return Stream.empty();
	}

	private Stream<TreeRecording> shrinkHead() {
		Stream<? extends SourceRecording> headCandidates = head.shrink();
		return headCandidates.map(this::replaceHead);
	}

	private TreeRecording replaceHead(SourceRecording headRecording) {
		return new TreeRecording(headRecording, children);
	}
}
