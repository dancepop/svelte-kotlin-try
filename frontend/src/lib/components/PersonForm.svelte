<script lang="ts">
	import { untrack } from 'svelte';
	import { formErrors } from '#lib/api/graphql.ts';
	import type { Person, PersonInput } from '#lib/api/types.ts';

	const FIELDS = ['name', 'bio'];

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
	let fieldErrors = $state<Record<string, string>>({});
	let banner = $state<string | null>(null);

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		saving = true;
		fieldErrors = {};
		banner = null;
		try {
			await onsave({ name, bio: bio || null });
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
		<label class="fieldset-legend" for="name">Name</label>
		<input
			id="name"
			class="input w-full"
			class:input-error={fieldErrors.name}
			bind:value={name}
			required
			maxlength="200"
		/>
		{#if fieldErrors.name}<p class="text-error text-sm">{fieldErrors.name}</p>{/if}
	</div>

	<div class="fieldset">
		<label class="fieldset-legend" for="bio">Bio</label>
		<textarea
			id="bio"
			class="textarea w-full"
			class:textarea-error={fieldErrors.bio}
			rows="5"
			bind:value={bio}
			maxlength="5000"></textarea>
		{#if fieldErrors.bio}<p class="text-error text-sm">{fieldErrors.bio}</p>{/if}
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
