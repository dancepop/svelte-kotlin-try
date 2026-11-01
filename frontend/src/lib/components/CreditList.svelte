<script lang="ts">
	import { resolve } from '$app/paths';
	import { errorMessage } from '#lib/api/graphql.ts';
	import { addCredit, removeCredit } from '#lib/api/movies.ts';
	import type { CreditType, MovieCredit, PersonSummary } from '#lib/api/types.ts';
	import PersonPicker from './PersonPicker.svelte';

	let {
		movieId,
		type,
		heading,
		credits,
		onchanged
	}: {
		movieId: string;
		type: CreditType;
		heading: string;
		credits: MovieCredit[];
		onchanged: () => Promise<void>;
	} = $props();

	let picking = $state(false);
	let adding = $state(false);
	let removingId = $state<string | null>(null);
	let error = $state<string | null>(null);

	async function add(person: PersonSummary) {
		adding = true;
		error = null;
		try {
			await addCredit(movieId, person.id, type);
			await onchanged();
		} catch (e) {
			error = errorMessage(e);
		} finally {
			adding = false;
		}
	}

	async function remove(personId: string) {
		removingId = personId;
		error = null;
		try {
			await removeCredit(movieId, personId, type);
			await onchanged();
		} catch (e) {
			error = errorMessage(e);
		} finally {
			removingId = null;
		}
	}
</script>

<section class="rounded-box bg-base-200 mt-4 p-4">
	<div class="mb-2 flex items-center justify-between gap-2">
		<h2 class="text-lg font-semibold">{heading}</h2>
		<button
			type="button"
			class={['btn btn-sm', !picking && 'btn-primary']}
			onclick={() => (picking = !picking)}
		>
			{picking ? 'Done' : '+ Add'}
		</button>
	</div>

	{#if error}
		<p class="text-error mb-2 text-sm" role="alert">{error}</p>
	{/if}

	{#if credits.length > 0}
		<ul class="divide-base-300 divide-y">
			{#each credits as credit (credit.personId)}
				<li class="flex items-center justify-between gap-2 py-1">
					<a
						class="link link-hover wrap-anywhere"
						href={resolve('/people/[id]', { id: credit.personId })}
					>
						{credit.name}
					</a>
					<button
						type="button"
						class="btn btn-ghost btn-circle text-error text-xl"
						aria-label="Remove {credit.name}"
						title="Remove"
						disabled={removingId === credit.personId}
						onclick={() => remove(credit.personId)}
					>
						{#if removingId === credit.personId}
							<span class="loading loading-spinner loading-sm"></span>
						{:else}
							✕
						{/if}
					</button>
				</li>
			{/each}
		</ul>
	{:else}
		<p class="text-base-content/70">Nobody yet</p>
	{/if}

	{#if picking}
		<div class="rounded-box border-base-300 bg-base-100 mt-3 border p-3">
			<h3 class="font-medium">Add to {heading.toLowerCase()}</h3>
			<PersonPicker label="Search people to add to {heading}" disabled={adding} onpick={add} />
		</div>
	{/if}
</section>
