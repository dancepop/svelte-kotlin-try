<script lang="ts">
	let {
		pageNumber,
		pageSize,
		totalCount,
		onchange
	}: {
		pageNumber: number;
		pageSize: number;
		totalCount: number;
		onchange: (pageNumber: number) => void;
	} = $props();

	const lastPage = $derived(Math.ceil(totalCount / pageSize));
</script>

{#if lastPage > 1}
	<nav aria-label="Pages" class="mt-6 flex items-center justify-center gap-3">
		<button
			type="button"
			class="btn"
			disabled={pageNumber <= 1}
			onclick={() => onchange(pageNumber - 1)}
		>
			‹ Prev
		</button>
		<span>Page {pageNumber} of {lastPage}</span>
		<button
			type="button"
			class="btn"
			disabled={pageNumber >= lastPage}
			onclick={() => onchange(pageNumber + 1)}
		>
			Next ›
		</button>
	</nav>
{/if}
