<script lang="ts">
	let { message, onretry }: { message: string; onretry: () => void } = $props();
</script>

<div class="alert alert-error mb-4" role="alert">
	<span>{message}</span>
	<button type="button" class="btn btn-sm" onclick={onretry}>Retry</button>
</div>
