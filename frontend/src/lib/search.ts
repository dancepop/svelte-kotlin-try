// A filter starts a word, so `recast:x` stays part of the title search. Its value can follow a space,
// and is quoted or runs to the next space.
const FILTER = /(?<!\S)(cast|creator):\s*(?:"([^"]*)"|(\S*))/gi;

/** Splits `heat cast:david creator:"michael mann"` into its filters. If a filter is repeated, the last one wins. */
export function parseSearch(text: string): {
	search: string;
	castName: string;
	creatorName: string;
} {
	let castName = '';
	let creatorName = '';
	const search = text
		.replace(FILTER, (_match, filter: string, quoted?: string, plain?: string) => {
			const value = quoted ?? plain ?? '';
			if (filter.toLowerCase() === 'cast') castName = value;
			else creatorName = value;
			return '';
		})
		.replace(/\s+/g, ' ')
		.trim();
	return { search, castName, creatorName };
}
