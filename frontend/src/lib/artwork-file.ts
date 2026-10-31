const MAX_ARTWORK_BYTES = 5 * 1024 * 1024;
const ALLOWED_EXTENSION = /\.(jpe?g|png)$/i;

/** Returns what's wrong with the file, or null if it can be uploaded. */
export function checkArtworkFile(file: { name: string; size: number }): string | null {
	if (!ALLOWED_EXTENSION.test(file.name)) return 'Artwork must be a .jpg, .jpeg or .png file';
	if (file.size > MAX_ARTWORK_BYTES) return 'Artwork must be 5 MB or smaller';
	return null;
}
