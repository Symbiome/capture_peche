/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import AbstractFisholaService from "@/services/AbstractFisholaService";
import { Feedback } from "@/pojos/BackendPojos";

/**
 * Service allowing to create feedback (locally) and send them to server when connected to internet.
 */
export default class FeedbackService extends AbstractFisholaService {
  // Passed this delay (in MS), the feedback will be deleted (the service will not try to send it to server anymore)
  private static FEEDBACK_DELETION_DELAY_MS = 1000 * 60 * 60 * 24 * 14;
  constructor() {
    super();
  }

  /**
   * Stores the given feedback in local database.
   * It will be send to server later with a synchronisation mechanism.
   * @param feedback the feedbakc to store
   */
  static sendFeedback(feedback: Feedback): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      this.getDatabase()
        .offlineFeedbacks.put(feedback)
        .then(
          (id) => {
            console.info("Feedback saved locally", id);
            resolve();
            // Try to send the feedback right away as user may leave and not come back
            FeedbackService.syncFeedbacks();
          },
          (error) => {
            console.error(
              "Error while trying to locally save feedback",
              error,
              feedback
            );
            reject();
          }
        );
    });
  }

  /**
   * Tries to send all feedbacks saved locally to the server (and the delete them locally).
   */
  static syncFeedbacks() {
    this.getDatabase()
      .offlineFeedbacks.toCollection()
      .primaryKeys((feedbackIds: string[]) => {
        // Step 1: get offline feedbacks
        if (feedbackIds && feedbackIds.length > 0) {
          console.info("Offline feedbacks to send to server : ", feedbackIds);

          // Step 2: create promise for sending each feedback to server
          const allPromises: Promise<void>[] = [];
          feedbackIds.forEach((feedbackId) => {
            const promise = this.sendFeedbackToServer(feedbackId);
            allPromises.push(promise);
            promise.then(
              () => {
                console.info("Feedback sent to server", feedbackId);
                FeedbackService.deleteStoredFeedback(feedbackId);
              },
              (error) => {
                console.error("Failed to save feedback ", feedbackId, error);
              }
            );
          });

          // Step3: deleting old feedbacks
          feedbackIds.forEach((feedbackId) => {
            FeedbackService.getStoredFeedback(feedbackId).then((result) => {
              if (result.date) {
                const dirtySinceInMillis =
                  new Date().getTime() - result.date.getTime();
                if (
                  dirtySinceInMillis >
                  FeedbackService.FEEDBACK_DELETION_DELAY_MS
                ) {
                  console.info(
                    `Deleting ${feedbackId} which was not send since ${result.date}`
                  );
                  FeedbackService.deleteStoredFeedback(feedbackId);
                  return;
                }
              }
            });
          });

          if (allPromises.length > 0) {
            Promise.all(allPromises).then(
              () => {
                console.info("All feedbacks are sent to server");
              },
              (eee) => {
                console.error("Issue while trying to sync feedbacks ", eee);
              }
            );
          }
        }
      });
  }

  static sendFeedbackToServer(feedbackId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      FeedbackService.getStoredFeedback(feedbackId).then((feedback) => {
        // Check if feedback is too old to be sent
        if (feedback.date) {
          const dirtySinceInMillis =
            new Date().getTime() - feedback.date.getTime();
          if (dirtySinceInMillis > FeedbackService.FEEDBACK_DELETION_DELAY_MS) {
            console.info(
              `Deleting ${feedbackId} which was not send since ${feedback.date}`
            );
            FeedbackService.deleteStoredFeedback(feedbackId);
            reject("Feedback is too old");
            return;
          }
          // Send feedback
          this.backendPut("/v1/feedback", feedback).then(resolve, reject);
        }
      }, reject);
    });
  }

  private static getStoredFeedback(feedbackId: string): Promise<Feedback> {
    return new Promise<Feedback>((resolve, reject) => {
      this.getDatabase()
        .offlineFeedbacks.get(feedbackId)
        .then((item?: Feedback) => {
          if (item) {
            resolve(item);
          } else {
            reject();
          }
        }, reject);
    });
  }

  private static deleteStoredFeedback(feedbackId: string) {
    this.getDatabase().offlineFeedbacks.delete(feedbackId);
  }
}
