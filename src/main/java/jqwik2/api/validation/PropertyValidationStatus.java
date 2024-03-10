package jqwik2.api.validation;

public enum PropertyValidationStatus {

	/**
	 * Indicates that the validation of a property was <em>successful</em>.
	 */
	SUCCESSFUL {
		@Override
		public boolean isSuccessful() {
			return true;
		}
	},

	/**
	 * Indicates that the execution of a property was
	 * <em>aborted</em> before the actual property method could be run.
	 */
	ABORTED {
		@Override
		public boolean isAborted() {
			return true;
		}
	},

	/**
	 * Indicates that the execution of a property has <em>failed</em>.
	 */
	FAILED {
		@Override
		public boolean isFailed() {
			return true;
		}
	};

	public boolean isSuccessful() {
		return false;
	}

	public boolean isAborted() {
		return false;
	}

	public boolean isFailed() {
		return false;
	}
}
