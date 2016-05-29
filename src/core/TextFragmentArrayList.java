package core;

import java.util.ArrayList;

import org.apache.lucene.search.highlight.TextFragment;

/**
 * Meta class, used to merge consecutive added TextFragments,
 * if possible. 
 */
public class TextFragmentArrayList extends ArrayList<TextFragment> {
	
	private static final long serialVersionUID = -3229035962744948571L;

	public boolean add(TextFragment fragment) {
		if (this.isEmpty())  {
			return super.add(fragment);
		}
		
		int currentIndex = this.size() - 1;
		TextFragment currentFragment = this.get(currentIndex);
		if (!fragment.follows(currentFragment)) {
			return super.add(fragment);
		}
		currentFragment.merge(fragment);
		return false;
	}
}
