import { errorMessage } from './graphql.ts';

/**
 * Holds what a page fetched, plus its loading and error state. Create it while a component initialises.
 * It fetches straight away, and again when any reactive value `fetchData` reads before its first `await` changes.
 */
export class Loader<T> {
	data = $state.raw<T | null>(null);
	loading = $state(true);
	error = $state<string | null>(null);

	#fetchData: () => Promise<T>;
	#latest = 0;

	constructor(fetchData: () => Promise<T>) {
		this.#fetchData = fetchData;
		$effect(() => {
			void this.reload();
		});
	}

	async reload(): Promise<void> {
		// A newer request replaces this one, so a slow older answer can't overwrite it.
		const request = ++this.#latest;
		this.loading = true;
		this.error = null;
		try {
			const result = await this.#fetchData();
			if (request === this.#latest) this.data = result;
		} catch (error) {
			if (request === this.#latest) this.error = errorMessage(error);
		} finally {
			if (request === this.#latest) this.loading = false;
		}
	}
}
