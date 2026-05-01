import authService from './auth.service';
import type { FeedbackSummary } from './ai.service';
import api from '@/utils/api';

interface FeedbackData {
	userId: string;
	contentType: string;
	contentId: string;
	content: string;
}

class FeedbackService {
	/**
	 * Save feedback to the server
	 */
	async saveFeedback(
		contentId: string,
		summary: FeedbackSummary,
		userId: string,
	): Promise<boolean> {
		try {
			const authToken = authService.getToken();
			if (!authToken) {
				console.error('User not authenticated');
				return false;
			}

			// Use the userId passed as parameter
			if (!userId) {
				console.error('User ID not available');
				return false;
			}

			// Prepare feedback data
			const feedbackData: FeedbackData = {
				userId: userId,
				contentType: 'conversation',
				contentId: contentId,
				content: JSON.stringify({
					summary: summary.summary,
					commonErrors: summary.common_errors,
					improvementTips: summary.improvement_tips,
					attempts: summary.attempts || 0,
					avgScore: summary.avg_score || 0,
					maxScore: summary.max_score || 0,
					timestamp: new Date().toISOString(),
				}),
			};

			const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

			// Save feedback to server
			await api.post(`${apiUrl}/api/v1/learning/feedback`, feedbackData);
			return true;
		} catch (error) {
			console.error('Error saving feedback:', error);
			return false;
		}
	}
}

export default new FeedbackService();
