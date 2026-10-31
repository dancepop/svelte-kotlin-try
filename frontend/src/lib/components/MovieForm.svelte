<script lang="ts">
	import { untrack } from 'svelte';
	import { formErrors } from '#lib/api/graphql.ts';
	import type { Movie, MovieInput } from '#lib/api/types.ts';

	const FIELDS = ['title', 'synopsis', 'releaseDate', 'runtimeMinutes'];

	let {
		movie,
		submitLabel,
		onsave,
		oncancel
	}: {
		movie?: Movie;
		submitLabel: string;
		onsave: (input: MovieInput) => Promise<void>;
		oncancel?: () => void;
	} = $props();

	// The form starts from the movie's values, and from then on holds what the user types.
	const initial = untrack(() => movie);
	let title = $state(initial?.title ?? '');
	let synopsis = $state(initial?.synopsis ?? '');
	let releaseDate = $state(initial?.releaseDate ?? '');
	let runtimeMinutes = $state<number | null>(initial?.runtimeMinutes ?? null);

	let saving = $state(false);
	let fieldErrors = $state<Record<string, string>>({});
	let banner = $state<string | null>(null);

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		saving = true;
		fieldErrors = {};
		banner = null;
		try {
			// An empty input gives "", which isn't a date, so empty fields are sent as null.
			await onsave({
				title,
				synopsis: synopsis || null,
				releaseDate: releaseDate || null,
				runtimeMinutes
			});
		} catch (error) {
			({ fields: fieldErrors, banner } = formErrors(error, FIELDS));
		} finally {
			saving = false;
		}
	}
</script>

<form onsubmit={submit}>
	{#if banner}
		<p class="alert alert-error mb-4" role="alert">{banner}</p>
	{/if}

	<div class="fieldset">
		<label class="fieldset-legend" for="title">Title</label>
		<input
			id="title"
			class="input w-full"
			class:input-error={fieldErrors.title}
			bind:value={title}
			required
			maxlength="200"
		/>
		{#if fieldErrors.title}<p class="text-error text-sm">{fieldErrors.title}</p>{/if}
	</div>

	<div class="fieldset">
		<label class="fieldset-legend" for="synopsis">Synopsis</label>
		<textarea
			id="synopsis"
			class="textarea w-full"
			class:textarea-error={fieldErrors.synopsis}
			rows="5"
			bind:value={synopsis}
			maxlength="5000"></textarea>
		{#if fieldErrors.synopsis}<p class="text-error text-sm">{fieldErrors.synopsis}</p>{/if}
	</div>

	<div class="fieldset">
		<label class="fieldset-legend" for="release-date">Release date</label>
		<input
			id="release-date"
			type="date"
			class="input w-full"
			class:input-error={fieldErrors.releaseDate}
			bind:value={releaseDate}
		/>
		{#if fieldErrors.releaseDate}<p class="text-error text-sm">{fieldErrors.releaseDate}</p>{/if}
	</div>

	<div class="fieldset">
		<label class="fieldset-legend" for="runtime">Runtime (minutes)</label>
		<input
			id="runtime"
			type="number"
			class="input w-full"
			class:input-error={fieldErrors.runtimeMinutes}
			min="1"
			max="1000"
			bind:value={runtimeMinutes}
		/>
		{#if fieldErrors.runtimeMinutes}
			<p class="text-error text-sm">{fieldErrors.runtimeMinutes}</p>
		{/if}
	</div>

	<div class="mt-4 flex flex-wrap gap-2">
		<button type="submit" class="btn btn-primary" disabled={saving}>
			{saving ? 'Saving…' : submitLabel}
		</button>
		{#if oncancel}
			<button type="button" class="btn" disabled={saving} onclick={oncancel}>Cancel</button>
		{/if}
	</div>
</form>
