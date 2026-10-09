# Kết quả kiểm thử tự động

Sinh bởi `backend/scripts/report/test-summary.py` từ lần `./mvnw verify` lúc 09/10/2026 15:54. Test tích hợp chạy trên MySQL 8.4 thật (Testcontainers), không gọi mô hình ngôn ngữ thật.

## Tổng hợp theo module

| Module | Lớp test | Ca kiểm thử | Đạt | Lỗi | Bỏ qua | Thời gian (s) |
|---|---:|---:|---:|---:|---:|---:|
| Báo cáo vi phạm | 1 | 6 | 6 | 0 | 0 | 2.6 |
| Chatbot AI | 4 | 18 | 17 | 0 | 1 | 15.8 |
| Chương, đăng/hẹn giờ, trang đọc | 5 | 34 | 34 | 0 | 0 | 11.5 |
| Hạ tầng dùng chung (bảo mật, lưu ảnh, lỗi, tiện ích) | 6 | 46 | 46 | 0 | 0 | 5.5 |
| Quản lý người dùng | 3 | 18 | 18 | 0 | 0 | 1.0 |
| Tham số, nhật ký | 1 | 4 | 4 | 0 | 0 | 0.6 |
| Theo dõi, đánh giá, bình luận | 2 | 17 | 17 | 0 | 0 | 4.3 |
| Thông báo | 1 | 8 | 8 | 0 | 0 | 1.3 |
| Thể loại | 1 | 11 | 11 | 0 | 0 | 4.7 |
| Thống kê, xếp hạng | 3 | 13 | 13 | 0 | 0 | 1.5 |
| Toàn hệ thống (lược đồ, phân quyền) | 2 | 19 | 19 | 0 | 0 | 2.5 |
| Truyện, tìm kiếm, kiểm duyệt | 6 | 58 | 58 | 0 | 0 | 15.9 |
| Tài khoản, đăng nhập, hồ sơ | 4 | 24 | 24 | 0 | 0 | 82.2 |
| Đăng ký tác giả | 1 | 12 | 12 | 0 | 0 | 3.7 |
| **Tổng** | **40** | **288** | **287** | **0** | **1** | **153.0** |

## Độ phủ mã (JaCoCo)

Toàn bộ: **92.0%** số dòng, **74.4%** số nhánh. Ngưỡng bắt buộc trong `pom.xml`: mỗi package `*.service` phủ tối thiểu 60% số dòng.

| Package | Dòng | Nhánh |
|---|---:|---:|
| `auth.service` | 93.8% | 100.0% |
| `author.service` | 96.6% | 91.7% |
| `chapter.service` | 97.2% | 83.3% |
| `chatbot.service` | 90.8% | 71.7% |
| `genre.service` | 100.0% | 92.9% |
| `interaction.service` | 98.0% | 79.2% |
| `notification.service` | 82.9% | 80.0% |
| `report.service` | 100.0% | 84.8% |
| `stats.service` | 100.0% | 93.8% |
| `story.service` | 99.4% | 87.9% |
| `system.service` | 91.4% | 58.8% |
| `user.service` | 98.1% | 87.5% |

## Danh sách ca kiểm thử

### Báo cáo vi phạm

**ReportIntegrationTest**

- ✔ `resolving_hidesTheReportedContentWithTheGivenReason`
- ✔ `storyPage_offersTheReportButton_toLoggedInReadersOnly`
- ✔ `admin_seesPendingReports_andCanDismissOne`
- ✔ `report_isRefused_forGuests_invalidInput_andContentReadersCannotSee`
- ✔ `reader_reportsAStory_onceWhilePending`
- ✔ `resolving_aReportWhoseTargetIsAlreadyHidden_onlyClosesTheReport`

### Chatbot AI

**ChatbotAdminAndCleanupIntegrationTest**

- ✔ `cleanupJob_deletesOnlyConversationsInactiveLongerThanTheRetention`
- ✔ `dailyLimit_holdsEvenWhenOneUserSendsFromSeveralTabsAtOnce`
- ✔ `adminPage_showsUsageAndRecentNegativeFeedback`

**ChatbotCatalogServiceIntegrationTest**

- ✔ `storyDetail_ofHiddenOrUnknownStory_isNotFound_insteadOfAnError`
- ✔ `fallback_recognisesGenreAndTypeNamesInTheSentence`
- ✔ `search_returnsCompactItems_andCapsTheLimit`
- ✔ `search_relaxesKeywordFirst_thenChapterRange_thenStatus_andSaysSo`
- ✔ `similarStories_shareMostGenres_andExcludeTheStoryItselfAndNonPublicOnes`
- ✔ `search_neverReturnsDraftHiddenOrDeletedStories`

**ChatbotIntegrationTest**

- ✔ `reply_showsOnlyStoriesThatToolsReturned_builtFromTheDatabase`
- ✔ `history_isReloadedWithCards_andFeedbackCanBeGivenAndCleared`
- ✔ `guards_rejectGuestsLongMessagesDisabledChatAndDailyLimit_beforeCallingTheModel`
- ✔ `followUpQuestion_seesHistoryWithASummary_andMayReuseStoriesFromEarlierTurns`
- ✔ `conversationsAreOwnedByTheirUser`
- ✔ `plainTextAnswer_isAcceptedWithTheLastToolResults`
- ✔ `modelFailures_fallBackToKeywordSearch_andRecordTheReason`
- ✔ `systemPrompt_carriesGenresAndOutputFormat_andToolsAreRegistered`

**SpringAiToolCallingSmokeTest**

- ○ `model_callsToolAndAnswersFromItsResult`

### Chương, đăng/hẹn giờ, trang đọc

**ChapterPageApiIntegrationTest**

- ✔ `upload_appendsPagesInOrder_withRealDimensions_andLetsTheChapterBePublished`
- ✔ `reorder_renumbersPages_andRefusesListsThatDoNotMatchTheChapter`
- ✔ `concurrentUploads_toTheSameChapter_neverShareAPageNumber`
- ✔ `upload_stopsAtTheConfiguredPageLimit`
- ✔ `delete_closesTheGapInPageNumbers`
- ✔ `pages_areOffLimitsToOtherAuthors_readers_guests_andNovelChapters`
- ✔ `upload_rejectsFilesThatAreNotImages_whateverTheirNameSays`
- ✔ `lastPage_ofAPublishedChapter_cannotBeDeleted_butADraftMayBecomeEmpty`

**ChapterPublishJobIntegrationTest**

- ✔ `emptyChapter_canBeNeitherPublishedNorScheduled`
- ✔ `chapterOfAStoryThatIsNotPublic_isPublishedWithoutNotifyingAnyone`
- ✔ `jobRunThreeTimes_publishesADueChapterExactlyOnce`
- ✔ `dueChapter_thatTheAuthorJustUnscheduled_isNotPublished`
- ✔ `job_leavesChaptersThatAreNotDueYet`
- ✔ `publishNow_notifiesFollowersOnce_andCannotBeRepeated`

**ChapterReadTrackingIntegrationTest**

- ✔ `sameVisitor_isCountedSeparatelyForDifferentChapters`
- ✔ `authorReadingOwnStory_andPreviewOfUnpublishedChapter_areNotCounted`
- ✔ `guestReading_recordsNoProgress`
- ✔ `readingProgress_followsLatestOpenedChapter_andFeedsContinueReading`
- ✔ `view_isCountedOncePerVisitorPerChapter`

**StudioChapterIntegrationTest**

- ✔ `onlyDraftChapters_canBeDeleted_togetherWithTheirContent`
- ✔ `publishButton_onEmptyChapter_keepsTheDraftAndExplainsWhy`
- ✔ `publishedChapter_keepsItsNumber_andCannotBeEmptied_butContentIsEditable`
- ✔ `chapterNumber_mustBeUniqueWithinTheStory_andInRange`
- ✔ `author_cannotTouchChaptersOfAnotherAuthorsStory`
- ✔ `newChapterForm_suggestsNextNumber_andEditorMatchesStoryType`
- ✔ `publishButton_savesThenPublishes_andCountsTheChapterOnTheStory`
- ✔ `createNovelChapter_savesSanitizedDraft_thatOnlyTheAuthorCanPreview`
- ✔ `schedule_storesVietnamTimeAsUtc_rejectsThePast_andCanBeCancelled`

**ChapterStatusTest**

- ✔ `onlyPublishedChapter_isPubliclyReadable`
- ✔ `publishedChapter_cannotGoBackToDraft`
- ✔ `canTransitionTo_matchesDocumentedTable(ChapterStatus)[1]`
- ✔ `canTransitionTo_matchesDocumentedTable(ChapterStatus)[2]`
- ✔ `canTransitionTo_matchesDocumentedTable(ChapterStatus)[3]`
- ✔ `canTransitionTo_matchesDocumentedTable(ChapterStatus)[4]`

### Hạ tầng dùng chung (bảo mật, lưu ảnh, lỗi, tiện ích)

**GlobalExceptionHandlerTest**

- ✔ `clientDisconnect_isNotTreatedAsServerError_andRendersNothing`
- ✔ `businessError_keepsItsOwnStatusAndMessageKey`
- ✔ `unexpectedError_onPage_rendersErrorPageWithStatus500`
- ✔ `unexpectedError_underApi_returnsJsonWithoutInternalDetails`

**SecurityRulesIntegrationTest**

- ✔ `mediaUrls_cannotEscapeTheStorageFolder(String)[1]`
- ✔ `mediaUrls_cannotEscapeTheStorageFolder(String)[2]`
- ✔ `mediaUrls_cannotEscapeTheStorageFolder(String)[3]`
- ✔ `mediaUrls_cannotEscapeTheStorageFolder(String)[4]`
- ✔ `stateChangingRequests_withoutCsrfToken_areRejected_andChangeNothing`
- ✔ `pages_carrySecurityHeaders`
- ✔ `adminApi_rejectsAuthors`
- ✔ `guests_getJson401_fromApi_butMayReadComments`
- ✔ `developerTools_areAdminOnly`

**ImageValidatorTest**

- ✔ `validate_rejectsFileOverConfiguredLimit`
- ✔ `validate_rejectsNonImageDisguisedByExtension`
- ✔ `validate_rejectsRealImageOfUnsupportedFormat`
- ✔ `validate_rejectsEmptyFile`
- ✔ `validate_readsFormatFromContent_notFromFileName`
- ✔ `validate_acceptsJpeg`
- ✔ `validate_acceptsWebp`

**LocalStorageServiceTest**

- ✔ `resolveUrl_returnsNull_whenNoImage`
- ✔ `storeImage_writesFileUnderDirectoryWithGeneratedName`
- ✔ `delete_removesStoredFile_andIgnoresMissingOne`
- ✔ `storeImage_rejectsDirectoryEscapingRoot(String)[1]`
- ✔ `storeImage_rejectsDirectoryEscapingRoot(String)[2]`
- ✔ `storeImage_writesNothing_whenFileIsNotAnImage`
- ✔ `delete_rejectsKeyEscapingRoot`

**HtmlSanitizerTest**

- ✔ `stripsEveryAttribute_soNoHandlerOrStyleSurvives`
- ✔ `dropsDisallowedTagsButKeepsTheirText`
- ✔ `nullOrBlankInput_becomesEmptyContent`
- ✔ `keepsFormattingTagsOfTheEditor`
- ✔ `removesDangerousElementsCompletely(String)[1]`
- ✔ `removesDangerousElementsCompletely(String)[2]`
- ✔ `removesDangerousElementsCompletely(String)[3]`
- ✔ `removesDangerousElementsCompletely(String)[4]`
- ✔ `removesDangerousElementsCompletely(String)[5]`
- ✔ `countsWordsOfVisibleTextOnly`

**SlugUtilsTest**

- ✔ `toSlug_cutsToMaxLength_withoutLeavingTrailingHyphen`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[1]`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[2]`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[3]`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[4]`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[5]`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[6]`
- ✔ `toSlug_stripsVietnameseDiacriticsAndPunctuation(String, String)[7]`
- ✔ `toSlug_returnsEmpty_whenNameHasNoLetterOrDigit`

### Quản lý người dùng

**AdminUserIntegrationTest**

- ✔ `ban_ofUnknownAccount_reportsErrorInsteadOfFailing`
- ✔ `userList_treatsLikeWildcardsInKeywordLiterally`
- ✔ `userList_isForAdminsOnly`
- ✔ `ban_thenUnban_changesStatus_andUnbanClearsTemporaryLock`
- ✔ `admin_cannotBanOwnAccount`
- ✔ `userList_filtersByKeywordRoleAndStatus`

**AdminUserRoleIntegrationTest**

- ✔ `theLastActiveAdmin_canNeitherBeDemotedNorBanned`
- ✔ `admin_cannotChangeTheirOwnRole`
- ✔ `changeRole_takesEffectInTheUsersCurrentSession`

**PasswordPolicyTest**

- ✔ `check_acceptsPasswordLongEnoughWithLetterAndDigit(String)[1]`
- ✔ `check_acceptsPasswordLongEnoughWithLetterAndDigit(String)[2]`
- ✔ `check_acceptsPasswordLongEnoughWithLetterAndDigit(String)[3]`
- ✔ `check_followsMinLengthFromSetting`
- ✔ `check_rejectsShortOrSingleClassPassword(String)[1]`
- ✔ `check_rejectsShortOrSingleClassPassword(String)[2]`
- ✔ `check_rejectsShortOrSingleClassPassword(String)[3]`
- ✔ `check_rejectsShortOrSingleClassPassword(String)[4]`
- ✔ `check_rejectsShortOrSingleClassPassword(String)[5]`

### Tham số, nhật ký

**AdminSystemIntegrationTest**

- ✔ `readOnlySetting_cannotBeChanged`
- ✔ `updatingASetting_takesEffectImmediately_andIsAudited`
- ✔ `invalidValues_areRefused_andLeaveTheSettingUntouched`
- ✔ `settingsPage_listsEveryParameterByGroup`

### Theo dõi, đánh giá, bình luận

**CommentApiIntegrationTest**

- ✔ `chapterComments_areSeparateFromStoryComments`
- ✔ `hiddenComment_neverLeavesTheServer`
- ✔ `writingComments_requiresLogin`
- ✔ `comment_isStoredAsPlainText_andCountedOnTheStory`
- ✔ `sameUser_mustWaitBetweenTwoComments`
- ✔ `author_canDeleteOwnComment_whichKeepsItsPlaceButLosesItsContent`
- ✔ `reply_toAReply_isAttachedToTheThreadRoot`
- ✔ `comments_ofNonPublicStory_are404`
- ✔ `comment_isRejected_whenTargetIsInvalid_andNothingIsCounted`

**InteractionApiIntegrationTest**

- ✔ `concurrentRatings_bySameUser_leaveExactlyOneRating_andAMatchingSum`
- ✔ `concurrentFollows_ofSameStory_allSucceed_andAreAllCounted`
- ✔ `rating_addsOnlyTheDifference_whenUserChangesTheirScore`
- ✔ `rating_rejectsOutOfRangeStars_ownStory_andNonPublicStory`
- ✔ `followedStory_appearsInLibrary_andDisappearsWhenHidden`
- ✔ `interactions_requireLogin_andAnswerGuestsWithJson`
- ✔ `follow_andUnfollow_areIdempotent_soFollowCountNeverDrifts`
- ✔ `follow_ofNonPublicOrMissingStory_is404`

### Thông báo

**NotificationIntegrationTest**

- ✔ `unreadCount_isServedToTheBell_andGuestsGetJson401`
- ✔ `opening_marksAsRead_andRedirectsToTheLink`
- ✔ `readAll_marksOnlyTheUsersOwnNotifications`
- ✔ `notifyFollowers_writesOneNotificationPerFollower_inOneStatement`
- ✔ `notification_ofSomeoneElse_cannotBeOpened`
- ✔ `replyToAComment_notifiesItsAuthor_butNotWhenReplyingToOneself`
- ✔ `list_buildsTheSentenceFromTypeAndArguments`
- ✔ `opening_neverRedirectsOutsideTheApplication`

### Thể loại

**AdminGenreIntegrationTest**

- ✔ `delete_keepsGenreThatStoriesStillUse`
- ✔ `create_generatesSlugFromVietnameseName`
- ✔ `genreList_showsSeededGenres`
- ✔ `genreAdministration_isForAdminsOnly`
- ✔ `editForm_isPrefilled_andUnknownGenreGives404`
- ✔ `create_rejectsNameWithoutAnyLetterOrDigit`
- ✔ `update_changesFields_butKeepsSlug`
- ✔ `newGenre_showsUpInStoryFilterRightAway_thoughGenreListIsCached`
- ✔ `update_allowsKeepingOwnName_butRejectsAnotherGenresName`
- ✔ `create_rejectsNameThatDuplicatesOrSlugifiesToExistingGenre`
- ✔ `delete_removesUnusedGenre`

### Thống kê, xếp hạng

**AdminDashboardIntegrationTest**

- ✔ `charts_coverThirtyDays_andCountPublicStoriesPerGenre`
- ✔ `overview_countsOnlyWhatReadersCanSee`
- ✔ `dashboardPage_andChartApi_areForAdmins`

**AuthorStatsIntegrationTest**

- ✔ `overview_ofAnAuthorWithoutStories_isAllZeros`
- ✔ `statsPages_andChartApi_showTheAuthorsOwnNumbers`
- ✔ `overview_sumsTheAuthorsLiveStoriesOnly`
- ✔ `dailyViews_coverThirtyDaysWithZerosForQuietDays`
- ✔ `topChapters_listPublishedChaptersByViews`

**RankingServiceIntegrationTest**

- ✔ `viewRankings_onlyCountViewsInsideTheirWindow`
- ✔ `ranking_isServedFromCache_untilItExpires`
- ✔ `ratingRanking_ignoresStoriesWithTooFewRatings`
- ✔ `followRanking_ordersByFollowCount`
- ✔ `viewRanking_skipsStoriesThatAreNoLongerPublic`

### Toàn hệ thống (lược đồ, phân quyền)

**SchemaIntegrationTest**

- ✔ `seedData_containsAdminGenresAndSettings`
- ✔ `authorRequest_rejectsSecondPendingRequestOfSameUser`
- ✔ `authorRequest_allowsNewPendingRequestAfterRejection`
- ✔ `coreAggregates_persistAndReloadWithAuditColumns`
- ✔ `storyCounters_changeThroughAtomicUpdate_andSurviveEntitySave`

**SecurityAccessIntegrationTest**

- ✔ `unknownPath_returnsApplicationErrorPageInsteadOfLoginRedirect`
- ✔ `health_isOpenToGuests`
- ✔ `studio_rendersLayoutForAuthor`
- ✔ `api_answersGuestsWithJsonInsteadOfLoginRedirect`
- ✔ `protectedAreas_redirectGuestsToLogin`
- ✔ `login_failsWithWrongPassword`
- ✔ `readerArea_isOpenToAuthorAndAdminThroughRoleHierarchy`
- ✔ `admin_rendersLayoutForAdmin`
- ✔ `login_succeedsWithSeededAdminAccount`
- ✔ `studio_rejectsReaderAndAdmin`
- ✔ `home_isOpenToGuests`
- ✔ `post_withoutCsrfToken_isForbidden`
- ✔ `admin_rejectsReaderAndAuthor`
- ✔ `loginPage_rendersForGuests`

### Truyện, tìm kiếm, kiểm duyệt

**ModerationIntegrationTest**

- ✔ `hidingAStory_requiresAReason_andOnlyAppliesToPublishedStories`
- ✔ `moderationScreens_areForAdminsOnly`
- ✔ `hidingAChapter_adjustsTheStoryChapterCount_andUnhidingDoesNotNotifyAgain`
- ✔ `hidingAComment_withholdsItsContent_andAdjustsTheCommentCount`
- ✔ `adminStoryList_showsEveryStoryIncludingDraftsAndHidden`
- ✔ `hidingAStory_removesItFromReaders_notifiesTheAuthor_andRebuildsRankings`

**QueryCountIntegrationTest**

- ✔ `storyDetail_andReader_doNotQueryOncePerChapterOrGenre`
- ✔ `homeAndListPages_doNotQueryOncePerStory`

**StoryCatalogQueryServiceIntegrationTest**

- ✔ `search_sortsByChosenCriterion`
- ✔ `search_findsWordsThatOnlyAppearInDescription`
- ✔ `search_matchesTitleTypedWithoutDiacritics`
- ✔ `storyDetail_ofDraft_isVisibleOnlyToItsAuthorAndAdmins`
- ✔ `search_requiresEverySelectedGenre_andHonoursExcludedGenres`
- ✔ `search_treatsLikeWildcardsInKeywordLiterally`
- ✔ `findCardsByIds_keepsGivenOrder_andSkipsStoriesNoLongerPublic`
- ✔ `card_carriesGenresInDisplayOrder_andCoverUrl`
- ✔ `search_returnsOnlyPubliclyVisibleStories`
- ✔ `search_filtersByTypeStatusAndChapterRange`

**StoryPagesIntegrationTest**

- ✔ `home_listsPublishedStories`
- ✔ `missingStoryOrChapter_is404`
- ✔ `storyList_showsMatchingPublishedStory_butNotDraftWithSameKeyword`
- ✔ `comicChapter_rendersPagesInOrderWithDimensions`
- ✔ `nonPublicStory_is404ForStrangers_butPreviewableByAuthorAndAdmin(StoryVisibility)[1]`
- ✔ `nonPublicStory_is404ForStrangers_butPreviewableByAuthorAndAdmin(StoryVisibility)[2]`
- ✔ `genrePage_listsStoriesOfGenre_andUnknownGenreGives404`
- ✔ `rankings_renderForEveryCriterion(RankingType)[1]`
- ✔ `rankings_renderForEveryCriterion(RankingType)[2]`
- ✔ `rankings_renderForEveryCriterion(RankingType)[3]`
- ✔ `rankings_renderForEveryCriterion(RankingType)[4]`
- ✔ `rankings_renderForEveryCriterion(RankingType)[5]`
- ✔ `novelChapter_rendersStoredHtml_withLinksToNeighbouringPublishedChapters`
- ✔ `libraryAndHistory_requireLogin`
- ✔ `unpublishedChapter_is404ForReaders_butPreviewableByAuthor(ChapterStatus)[1]`
- ✔ `unpublishedChapter_is404ForReaders_butPreviewableByAuthor(ChapterStatus)[2]`
- ✔ `unpublishedChapter_is404ForReaders_butPreviewableByAuthor(ChapterStatus)[3]`
- ✔ `storyDetail_showsPublishedChaptersOnly`

**StudioStoryIntegrationTest**

- ✔ `studio_isForAuthorsOnly_notEvenAdmins(Role)[1]`
- ✔ `studio_isForAuthorsOnly_notEvenAdmins(Role)[2]`
- ✔ `storyHiddenByAdmin_cannotBeRepublishedByItsAuthor_whoSeesTheReason`
- ✔ `create_makesADraftWithSlugGenresAndCover_thatReadersCannotSeeYet`
- ✔ `create_showsFieldErrors_andCreatesNothing`
- ✔ `update_changesFields_butKeepsSlugAndCover`
- ✔ `delete_isSoft_andHidesTheStoryFromEveryone`
- ✔ `author_cannotTouchAnotherAuthorsStory`
- ✔ `update_cannotChangeType_onceTheStoryHasAChapter`
- ✔ `publish_needsCoverAndGenre_thenMakesTheStoryPublic`
- ✔ `create_withATitleAlreadyUsed_getsANumberedSlug`

**StoryAccessPolicyTest**

- ✔ `canRead(StoryVisibility, ChapterStatus, boolean, boolean, boolean)[1]`
- ✔ `canRead(StoryVisibility, ChapterStatus, boolean, boolean, boolean)[2]`
- ✔ `canRead(StoryVisibility, ChapterStatus, boolean, boolean, boolean)[3]`
- ✔ `canRead(StoryVisibility, ChapterStatus, boolean, boolean, boolean)[4]`
- ✔ `canRead(StoryVisibility, ChapterStatus, boolean, boolean, boolean)[5]`
- ✔ `canRead(StoryVisibility, ChapterStatus, boolean, boolean, boolean)[6]`
- ✔ `canView(StoryVisibility, boolean, boolean, boolean, boolean, boolean)[1]`
- ✔ `canView(StoryVisibility, boolean, boolean, boolean, boolean, boolean)[2]`
- ✔ `canView(StoryVisibility, boolean, boolean, boolean, boolean, boolean)[3]`
- ✔ `canView(StoryVisibility, boolean, boolean, boolean, boolean, boolean)[4]`
- ✔ `canView(StoryVisibility, boolean, boolean, boolean, boolean, boolean)[5]`

### Tài khoản, đăng nhập, hồ sơ

**LoginLockoutIntegrationTest**

- ✔ `successfulLogin_resetsFailureCounter_andRecordsLoginTime`
- ✔ `bannedAccount_cannotLogIn_evenWithCorrectPassword`
- ✔ `account_isTemporarilyLocked_afterTooManyWrongPasswords`
- ✔ `loginPage_explainsEachFailureReason_andTreatsUnknownCodeAsBadCredentials`
- ✔ `attemptsWhileLocked_doNotExtendTheLock`
- ✔ `login_acceptsEmailInPlaceOfUsername`

**ProfileIntegrationTest**

- ✔ `changePassword_replacesPassword`
- ✔ `updateProfile_savesFields_storesAvatar_andRemovesReplacedAvatar`
- ✔ `updateProfile_rejectsFakeImage_andSavesNothing`
- ✔ `profilePages_requireLogin`
- ✔ `profilePage_showsAccountAndPrefilledForm`
- ✔ `changePassword_reportsEveryProblemAtOnce_andKeepsOldPassword`
- ✔ `changePassword_rejectsReusingCurrentPassword`
- ✔ `updateProfile_withoutChoosingAFile_keepsCurrentAvatar`

**RegistrationIntegrationTest**

- ✔ `register_changesSessionId_soAPreSetSessionCannotBeHijacked`
- ✔ `registerPage_isShownToGuests_andSkippedForLoggedInUsers`
- ✔ `register_rejectsPasswordWithoutDigit`
- ✔ `register_createsReaderAccountAndLogsIn`
- ✔ `register_reportsEveryProblemAtOnce_andCreatesNothing`
- ✔ `register_rejectsUsernameThatCouldCollideWithAnEmail`

**SessionRefreshIntegrationTest**

- ✔ `bannedUser_callingApi_receivesJson401_notARedirect`
- ✔ `renamedUser_seesNewDisplayNameInTheSameSession`
- ✔ `readerPromotedToAuthor_entersStudioWithoutLoggingInAgain`
- ✔ `bannedUser_isLoggedOutOnTheNextRequest_withTheReasonShown`

### Đăng ký tác giả

**AuthorRequestIntegrationTest**

- ✔ `approve_isRefused_whenAnotherAuthorTookThePenNameMeanwhile`
- ✔ `approve_makesTheReaderAnAuthor_inTheSessionTheyAreAlreadyLoggedInWith`
- ✔ `reader_submitsRequest_andThenSeesItPendingInsteadOfTheForm`
- ✔ `penName_cannotDuplicateAnAuthor_orAnotherPendingRequest_evenInDifferentCase`
- ✔ `rejectedReader_seesTheReason_andMayResubmitOnlyAfterTheCooldown`
- ✔ `approve_twice_createsOnlyOneProfile`
- ✔ `onlyReaders_maySubmit(Role)[1]`
- ✔ `onlyReaders_maySubmit(Role)[2]`
- ✔ `reviewScreens_areForAdminsOnly_andListPendingRequests`
- ✔ `submit_showsFieldErrors_andStoresNothing`
- ✔ `secondRequest_whileOneIsPending_isRefused`
- ✔ `reject_requiresAReason`

