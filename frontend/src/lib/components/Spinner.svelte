<script lang="ts">
	let { label = 'Loading…' }: { label?: string } = $props();
</script>

<div class="flex items-center gap-2 py-2" role="status">
	<span class="loading loading-spinner text-primary" aria-hidden="true"></span>
	<span class="text-base-content/70">{label}</span>
</div>
