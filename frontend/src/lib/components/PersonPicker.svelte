<script lang="ts">
	import { resolve } from '$app/paths';
	import { errorMessage } from '#lib/api/graphql.ts';
	import { listPeople } from '#lib/api/people.ts';
	import type { PersonSummary } from '#lib/api/types.ts';
	import Spinner from './Spinner.svelte';

	let {
		label,
		disabled,
		onpick
	}: {
		label: string;
		disabled: boolean;
		onpick: (person: PersonSummary) => void;
	} = $props();

	let text = $state('');
	let results = $state.raw<PersonSummary[] | null>(null);
	let searching = $state(false);
	let error = $state<string | null>(null);
	// The person last clicked, so only their button shows the spinner while `disabled`.
	let pickedId = $state<string | null>(null);

	async function search(event: SubmitEvent) {
		event.preventDefault();
		searching = true;
		error = null;
		try {
			({ items: results } = await listPeople({ search: text.trim(), page: 0, pageSize: 10 }));
		} catch (e) {
			error = errorMessage(e);
		} finally {
			searching = false;
		}
	}
</script>

<form class="join mt-2 w-full" onsubmit={search}>
	<input
		type="search"
		class="input join-item w-full"
		aria-label={label}
		placeholder="Type a name, then press Enter"
		bind:value={text}
	/>
	<button type="submit" class="btn btn-primary join-item" disabled={searching}>Search</button>
</form>

{#if searching}
	<Spinner label="Searching…" />
{:else if error}
	<p class="text-error mt-2 text-sm" role="alert">{error}</p>
{:else if results?.length === 0}
	<p class="text-base-content/70 mt-2">
		Nobody found. You can <a class="link" href={resolve('/people/new')}>add a new person</a>.
	</p>
{:else if results}
	<ul class="divide-base-300 mt-2 divide-y">
		{#each results as person (person.id)}
			<li class="flex items-center justify-between gap-2 py-2">
				<span class="wrap-anywhere">{person.name}</span>
				<button
					type="button"
					class="btn btn-sm btn-primary btn-outline"
					{disabled}
					onclick={() => {
						pickedId = person.id;
						onpick(person);
					}}
				>
					{#if disabled && pickedId === person.id}
						<span class="loading loading-spinner loading-xs"></span> Adding…
					{:else}
						+ Add
					{/if}
				</button>
			</li>
		{/each}
	</ul>
{/if}
