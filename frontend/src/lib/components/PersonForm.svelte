<script lang="ts">
	import { untrack } from 'svelte';
	import { errorMessage } from '#lib/api/graphql.ts';
	import type { Person, PersonInput } from '#lib/api/types.ts';

	let {
		person,
		submitLabel,
		onsave,
		oncancel
	}: {
		person?: Person;
		submitLabel: string;
		onsave: (input: PersonInput) => Promise<void>;
		oncancel?: () => void;
	} = $props();

	// The form starts from the person's values, and from then on holds what the user types.
	const initial = untrack(() => person);
	let name = $state(initial?.name ?? '');
	let bio = $state(initial?.bio ?? '');

	let saving = $state(false);
	let banner = $state<string | null>(null);

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		saving = true;
		banner = null;
		try {
			await onsave({ name, bio: bio || null });
		} catch (error) {
			banner = errorMessage(error);
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
		<label class="fieldset-legend" for="name">Name</label>
		<input id="name" class="input w-full" bind:value={name} required maxlength="200" />
	</div>

	<div class="fieldset">
		<label class="fieldset-legend" for="bio">Bio</label>
		<textarea id="bio" class="textarea w-full" rows="5" bind:value={bio} maxlength="5000"
		></textarea>
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
