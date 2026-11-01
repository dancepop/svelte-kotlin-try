<script lang="ts">
	import { errorMessage } from '#lib/api/graphql.ts';
	import { uploadArtwork } from '#lib/api/upload.ts';

	const MAX_ARTWORK_BYTES = 5 * 1024 * 1024;
	const ALLOWED_EXTENSION = /\.(jpe?g|png)$/i;

	let { movieId, onuploaded }: { movieId: string; onuploaded: (url: string) => void } = $props();

	let uploading = $state(false);
	let progress = $state(0);
	let error = $state<string | null>(null);

	/** Returns what's wrong with the file, or null if it can be uploaded. */
	function checkArtworkFile(file: File): string | null {
		if (!ALLOWED_EXTENSION.test(file.name)) return 'Artwork must be a .jpg, .jpeg or .png file';
		if (file.size > MAX_ARTWORK_BYTES) return 'Artwork must be 5 MB or smaller';
		return null;
	}

	async function upload(event: Event) {
		const input = event.currentTarget as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;

		error = checkArtworkFile(file);
		if (error) {
			input.value = '';
			return;
		}

		uploading = true;
		progress = 0;
		try {
			onuploaded(await uploadArtwork(movieId, file, (fraction) => (progress = fraction)));
		} catch (e) {
			error = errorMessage(e);
		} finally {
			// Allow reupload of same file
			uploading = false;
			input.value = '';
		}
	}
</script>

<div class="fieldset mt-3">
	<label class="fieldset-legend" for="artwork-file">Upload artwork</label>
	<input
		id="artwork-file"
		type="file"
		class="file-input w-full"
		accept=".jpg,.jpeg,.png"
		disabled={uploading}
		onchange={upload}
	/>
	<p class="text-base-content/70 text-sm">.jpg, .jpeg or .png, up to 5 MB</p>

	{#if uploading}
		<progress class="progress progress-primary w-full" max="1" value={progress}></progress>
		<!-- All the bytes have been sent at 100%, but the response only comes once the file is stored. -->
		<span class="text-base-content/70 text-sm" role="status">
			{progress < 1 ? `${Math.round(progress * 100)}%` : 'Finishing…'}
		</span>
	{/if}

	{#if error}
		<p class="text-error text-sm" role="alert">{error}</p>
	{/if}
</div>
