package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public class TreeShrinker {
	private final SourceRecording head;
	private final List<SourceRecording> children;

	public TreeShrinker(OldTreeRecording treeRecording) {
		this.head = treeRecording.head();
		this.children = treeRecording.children();
	}

	Stream<OldTreeRecording> shrink() {
		return Stream.concat(
			shrinkHead(),
			shrinkChildren()
		);
	}

	private Stream<OldTreeRecording> shrinkChildren() {
		if (children.isEmpty()) {
			return Stream.empty();
		}
		return Stream.concat(
			shrinkChildrenIndividually(),
			reorderChildren()
		).map(this::replaceChildren);
	}

	private Stream<List<SourceRecording>> shrinkChildrenIndividually() {
		return IntStream.range(0, children.size() - 1)
						.boxed()
						.flatMap(this::shrinkChild);
	}

	private Stream<List<SourceRecording>> shrinkChild(int index) {
		SourceRecording child = children.get(index);
		return child.shrink().map(shrunkChild -> replaceChild(index, shrunkChild));
	}

	private List<SourceRecording> replaceChild(int index, SourceRecording shrunkChild) {
		List<SourceRecording> shrunkChildren = new ArrayList<>(children);
		shrunkChildren.set(index, shrunkChild);
		return shrunkChildren;
	}

	private OldTreeRecording replaceChildren(List<SourceRecording> childrenRecordings) {
		return new OldTreeRecording(head, childrenRecordings);
	}

	private Stream<List<SourceRecording>> reorderChildren() {
		List<SourceRecording> reorderedChildren = new ArrayList<>(children);
		Collections.sort(reorderedChildren);
		return Stream.of(reorderedChildren);
	}

	private Stream<OldTreeRecording> shrinkHead() {
		Stream<? extends SourceRecording> headCandidates = head.shrink();
		return headCandidates.map(this::replaceHead);
	}

	private OldTreeRecording replaceHead(SourceRecording headRecording) {
		return new OldTreeRecording(headRecording, children);
	}
}
