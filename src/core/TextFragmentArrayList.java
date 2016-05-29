package core;

import java.util.ArrayList;

import org.apache.lucene.search.highlight.TextFragment;

/**
 * Meta class, used to merge consecutive added TextFragments,
 * if possible. Overrides the 'add' operation, which now has O(n) complexity.
 * This should be used for small lists.
 */
public class TextFragmentArrayList extends ArrayList<TextFragment> {
	
	private static final long serialVersionUID = -3229035962744948571L;

	public boolean add(TextFragment fragment) {
		if (this.isEmpty())  {
			return super.add(fragment);
		}
		TextFragment currentFragment = this.stream().filter(value -> fragment.follows(value))
				.findFirst().orElse(null);
		if (currentFragment == null) {
			return super.add(fragment);
		}
		currentFragment.merge(fragment);
		return false;
	}
}
