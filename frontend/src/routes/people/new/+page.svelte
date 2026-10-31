<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { createPerson } from '#lib/api/people.ts';
	import type { PersonInput } from '#lib/api/types.ts';
	import PersonForm from '#lib/components/PersonForm.svelte';

	async function save(input: PersonInput) {
		const id = await createPerson(input);
		await goto(resolve('/people/[id]', { id }));
	}
</script>

<svelte:head>
	<title>New person</title>
</svelte:head>

<div class="max-w-2xl">
	<h1 class="text-3xl font-bold">New person</h1>
	<p class="text-base-content/70 mt-1 mb-4">
		Add them to a movie's cast or creators from the movie's page.
	</p>
	<PersonForm submitLabel="Create person" onsave={save} />
</div>
