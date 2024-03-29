package com.xuni.core.group.domain;

import com.xuni.core.common.exception.NotPermissionException;
import com.xuni.core.group.domain.exception.CapacityOutOfBoundException;
import com.xuni.core.group.domain.exception.GroupJoinException;
import com.xuni.core.group.domain.exception.GroupStartException;
import com.xuni.core.group.domain.exception.NotAppropriateGroupStatusException;
import com.xuni.core.common.domain.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.xuni.core.common.exception.CommonExceptionMessage.BAD_REQUEST;
import static com.xuni.core.group.domain.GroupMessage.*;
import static com.xuni.core.group.domain.GroupStatus.*;
import static com.xuni.core.group.domain.exception.GroupExceptionMessage.INCORRECT_PERIOD;
import static com.xuni.core.group.domain.exception.GroupExceptionMessage.INCORRECT_TIME;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 그룹 도메인 규칙을 검증합니다.
 */

class GroupTest {

    protected static Group makeTestGroup(Integer capacity) {
        return new Group(
                "test-group",
                Period.of(LocalDate.now(), LocalDate.of(2023, 12, 31)),
                Time.of(LocalTime.MIDNIGHT, LocalTime.NOON),
                new Capacity(capacity),
                Study.of("UUID","자바의 정석", Category.JAVA),
                new Host(1l, "재헌"));
    }

    @DisplayName("초기화 된 Group 인스터스의 " +
            "Group Status 는 GATHERING" +
            "그룹을 만든 사람(Host)는 참여자 목록에 포함됩니다.")
    @Test
    void check_initialized_group_status() {
        Group group = makeTestGroup(5);

        assertThat(group.getGroupStatus()).isEqualTo(GroupStatus.GATHERING);

        assertThat(group.getGroupMembers().stream()
                .anyMatch(groupMember -> groupMember.hasEqualId(group.getHost().getHostId()))).isTrue();
    }

    @DisplayName("스터디 그룹의 인원은 최소 1인에서 최대 20인 까지 가능합니다. " +
            "이외의 값을 입력할 경우 CapacityOutOfBoundException 발생합니다.")
    @ParameterizedTest(name = "[{index}] capacity = {0}")
    @ValueSource(ints = {-1, 0, 21})
    void check_group_capacity(Integer capacity) {
        Group group = makeTestGroup(capacity);
        assertThatThrownBy(() -> group.checkCapacityRange())
                .isInstanceOf(CapacityOutOfBoundException.class);
    }

    @DisplayName("초기 가입")
    @Test
    void join_group_success() {
        //given
        Group group = makeTestGroup(5);
        GroupMember groupMember = new GroupMember(2l, "유니",group);
        group.join(groupMember);


        List<GroupMember> groupMembers = group.getGroupMembers();

        assertThat(groupMembers.contains(groupMember)).isTrue();
        assertThat(groupMembers.size()).isEqualTo(2);
    }

    @DisplayName("재가입")
    @Test
    void join_group_success_rejoin() {
        //given
        Group group = makeTestGroup(5);
        GroupMember groupMember = new GroupMember(2l, "유니",group);
        group.join(groupMember);

        GroupMember findGroupMember = group.getGroupMembers().stream()
                .filter(g -> g.hasEqualId(2l)).findFirst().get();

        group.leave(2l);

        //when - then
        assertThatCode(() -> group.join(findGroupMember)).doesNotThrowAnyException();
        assertThat(findGroupMember.hasNotLeft()).isTrue();
        assertThat(group.getGroupMembers().size()).isEqualTo(2);

    }

    // 그룹 입장 규칙
    @DisplayName("이미 들어가 있는 사용자가 그룹에 참여를 시도할 경우 " +
            "GroupJoinException 예외 발생 " +
            "예외 메시지 발생 ")
    @Test
    void join_group_fail_cause_already_join() {
        //given
        Group group = makeTestGroup(5);
        GroupMember groupMember = new GroupMember(1l, "유니", group);

        //when - then
        assertThatThrownBy(() -> group.join(groupMember)).isInstanceOf(GroupJoinException.class)
                .hasMessage(ALREADY_JOIN);
    }

    @DisplayName("남은 자리가 없는 그룹에 참여를 시도할 경우 " +
            "GroupJoinException 예외 발생 " +
            "예외 메시지 발생 ")
    @Test
    void join_group_fail_cause_left_capacity_is_0() {
        Group group = makeTestGroup(1);
        GroupMember groupMember = new GroupMember(2l, "이재헌", group);

        assertThatThrownBy(() -> group.join(groupMember)).isInstanceOf(GroupJoinException.class)
                .hasMessage(NOT_LEFT_CAPACITY);
    }

    @DisplayName("모집 중 상태가 아닌 그룹에 참여를 시도할 경우 " +
            "GroupJoinException 예외 발생 " +
            "예외 메시지 발생 ")
    @Test
    void join_group_fail_cause_group_status_not_gathering() {
        //given
        Group group = makeTestGroup(2);
        group.closeRecruitment(1l);

        //when - then
        GroupMember groupMember = new GroupMember(2l, "유니",group);
        assertThatThrownBy(() -> group.join(groupMember))
                .isInstanceOf(GroupJoinException.class);
    }

    @DisplayName("그룹 호스트가 아닌 사람은 그룹 모집을 마감하려 할 경우 " +
            "권한 없음 예외가 발생합니다.")
    @Test
    void close_recruitment_of_group_fail_cause_is_not_host() {
        Group group = makeTestGroup(2);

        assertThatThrownBy(() -> group.closeRecruitment(2l))
                .isInstanceOf(NotPermissionException.class)
                .hasMessage(NOT_PERMISSION);
    }

    @DisplayName("그룹 상태가 GATHERING 이 아닐 경우 그룹 모집을 마감할 수 없습니다. " +
            "권한 없음 예외가 발생합니다.")
    @ParameterizedTest
    @EnumSource(names = {"GATHER_COMPLETE", "START", "END"})
    void close_recruitment_of_group_fail_cause_inappropriate(GroupStatus groupStatus) {
        Group group = Group.builder()
                .capacity(new Capacity(5))
                .host(new Host(1l, "재헌"))
                .build();

        group.changeGroupStatusTo(groupStatus);

        assertThatThrownBy(() -> group.closeRecruitment(1l))
                .isInstanceOf(NotAppropriateGroupStatusException.class)
                .hasMessage(NOT_APPROPRIATE_GROUP_STATUS);
    }

    @DisplayName("그룹 시작이 성공적으로 작동했다면 " +
            "GroupStatus == START, " +
            "StudyChecks 에 memberId, chapterId, title, isDone 데이터가 추가 되어야 한다." +
            "그리고 isDone 초기값은 false 이다. " +
            "그룹 시작 전에 탈퇴한 사용자의 StudyCheck 은 만들어지지 않는다.")
    @Test
    void start_success() {
        //given
        Group group = Group.builder()
                .capacity(new Capacity(5))
                .host(new Host(1l, "재헌"))
                .build();

        //given 그룹에 멤버 추가
        group.join(new GroupMember(2l, "유니",group));
        group.join(new GroupMember(3l, "지니", group));
        group.leave(3l);

        //given 모집 마감
        group.closeRecruitment(1l);

        List<GroupTaskForm> studyCheckForms = new ArrayList<>();
        studyCheckForms.add(new GroupTaskForm(1l, "객체 지향의 사실과 오해"));
        //when
        group.start(1l, studyCheckForms);
        //then - 그룹 상태는 START 로 변경된다.
        assertThat(group.getGroupStatus()).isEqualTo(START);

        assertThat(group.getTasks().get(0).getTitle()).isEqualTo("객체 지향의 사실과 오해");
        assertThat(group.getTasks().get(0).getChapterId()).isEqualTo(1l);
        //then studyChecks 는 그룹에 참여중인 MemberId를 모두 가지고 있다. 탈퇴한 멤버는 가지고 있지 않다.
        List<Long> members = group.getTasks().stream().map(studyCheck -> studyCheck.getMemberId()).toList();
        assertThat(members).containsExactly(1l, 2l);
        //then studyChecks isDone 초기화 값은 false다.
        List<Boolean> isDones = group.getTasks().stream().map(studyCheck -> studyCheck.isDone()).toList();
        assertThat(isDones).containsOnly(false);

    }

    @DisplayName("그룹 호스트가 아닌 그룹 멤버가 그룹 시작을 할 경우 " +
            "NotPermissionException 예외가 발생합니다.")
    @Test
    void start_fail_cause_is_not_host() {
        Long hostId = 1l;
        Long groupMemberId = 2l;

        Group group = Group.builder()
                .capacity(new Capacity(5))
                .host(new Host(hostId, "재헌"))
                .build();

        group.changeGroupStatusTo(GATHER_COMPLETE);

        assertThatThrownBy(() -> group.start(groupMemberId, null))
                .isInstanceOf(NotPermissionException.class)
                .hasMessage(NOT_PERMISSION);
    }

    @DisplayName("그룹 상태가 GATHER_COMPLETE 가 아닌 상태에서 그룹 시작을 할 경우 " +
            " NotAppropriateGroupStatusException 예외가 발생합니다.")
    @ParameterizedTest
    @EnumSource(names = {"GATHERING","START","END"})
    void start_fail_cause_is_not_gather_complete_status(GroupStatus groupStatus) {
        Group group = Group.builder()
                .capacity(new Capacity(5))
                .host(new Host(1l, "재헌"))
                .build();

        group.changeGroupStatusTo(groupStatus);

        assertThatThrownBy(() -> group.start(1l, null))
                .isInstanceOf(NotAppropriateGroupStatusException.class)
                .hasMessage(NOT_APPROPRIATE_GROUP_STATUS);
    }

    @DisplayName("그룹 호스트가 아닌 그룹 멤버가 그룹 시작을 할 경우 " +
            "NotPermissionException 예외가 발생합니다.")
    @ParameterizedTest
    @NullSource
    @EmptySource
    void start_fail_cause_is_empty_or_null_study_check_form(List<GroupTaskForm> studyCheckForms) {
        Group group = Group.builder()
                .capacity(new Capacity(5))
                .host(new Host(1l, "재헌"))
                .build();

        group.changeGroupStatusTo(GATHER_COMPLETE);

        assertThatThrownBy(() -> group.start(1l, studyCheckForms))
                .isInstanceOf(GroupStartException.class);

    }

    @DisplayName("그룹 탈퇴는 탈퇴 플래그를 통해 구현하였다. " +
            "GroupMembers 프로퍼티 isLeft는 false -> true 변경 " +
            "탈퇴한 그룹 멤버는 GroupMembers 에는 여전히 존재 한다.(삭제 플래그 표시가 됐을 뿐) 고로 그룹 멤버 수는 탈퇴 전/후 동일하다." +
            "그룹 멤버 탈퇴 시 Left-Capacity 는 기존보다 1 증가해야 한다.")
    @Test
    void leave_group_success() {
        //given
        Group group = makeTestGroup(5);
        GroupMember groupMember = new GroupMember(2l, "유니",group);
        group.join(groupMember);


        Integer beforeCapacity = group.getCapacity().getLeftCapacity();
        int beforeGroupSize = group.getGroupMembers().size();
        //when
        group.leave(2l);
        //then - isLeft 프로퍼티가 True 로 변경되었는지 검증
        GroupMember findGroupMember = group.getGroupMembers().stream()
                .filter(g -> g.hasEqualId(2l))
                .findAny().get();
        assertThat(findGroupMember.getIsLeft()).isTrue();

        // then - 그룹 멤버 수에 변경이 없다는 것을 검증
        int afterGroupSize = group.getGroupMembers().size();
        assertThat(afterGroupSize).isEqualTo(beforeGroupSize);
        // then - 탈퇴 후, 가입 가능 정원이 1 증가한다는 것을 검증
        Integer afterCapacity = group.getCapacity().getLeftCapacity();
        assertThat(afterCapacity).isEqualTo(beforeCapacity + 1);
    }

    @DisplayName("호스트는 그룹을 탈퇴할 수 없다. 호스트가 그룹을 탈퇴하려는 경우, NotPermissionException 예외가 발생한다.")
    @Test
    void leave_group_fail_cause_host_leave() {
        //given
        Group group = makeTestGroup(5);
        long hostId = 1l;

        //when - then
        assertThatThrownBy(() -> group.leave(hostId))
                .isInstanceOf(NotPermissionException.class)
                .hasMessage(NOT_PERMISSION);

    }

    @DisplayName("그룹 상태가 END일 경우에는 그룹을 나가지 못한다. 시도할 경우 NotAppropriateGroupStatusException 예외가 발생한다.")
    @Test
    void leave_group_fail_cause_group_status_is_end() {
        //given
        List<GroupMember> groupMembers = new ArrayList<>();
        groupMembers.add(new GroupMember(2l, "포도", null));

        Group group = Group.builder()
                .host(new Host(1l, "자몽"))
                .capacity(new Capacity(5))
                .build();

        group.changeGroupStatusTo(END);

        //when - then
        assertThatThrownBy(() -> group.leave(2l))
                .isInstanceOf(NotAppropriateGroupStatusException.class)
                .hasMessage(NOT_APPROPRIATE_GROUP_STATUS);

    }

    @DisplayName("그룹 멤버가 아닌 사용자가 그룹을 탈퇴하려는 경우 IllegalArgumentException 예외가 발생한다.")
    @Test
    void leave_group_fail_cause_not_group_member() {
        //given
        Group group = makeTestGroup(5);

        //when - then
        assertThatThrownBy(() -> group.leave(2l))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_EXISTED_GROUP_MEMBER);

    }

    @DisplayName("이미 그룹을 나간 멤버가 다시 그룹 탈퇴를 하려는 경우 IllegalArgumentException 예외가 발생한다.")
    @Test
    void leave_group_fail_cause_repeated_leave() {
        //given
        Group group = makeTestGroup(5);
        GroupMember groupMember = new GroupMember(2l, "유니",group);
        group.join(groupMember);
        group.leave(2l);

        //when - then
        assertThatThrownBy(() -> group.leave(2l))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_EXISTED_GROUP_MEMBER);

    }

    @DisplayName("그룹 체크 성공 케이스")
    @Test
    void verify_check_rule_success() {
        //given - 그룹 시작 상태로 변경
        Group group = makeTestGroup(5);
        group.closeRecruitment(1l);
        group.start(1l, TestGroupFactory.studyCheckForms);
        //when
        group.doTask(2l, 1l);

        Task studyCheckAfterVerifyCheckRule = group.getTasks().stream()
                .filter(s -> s.isEqualMemberId(1l))
                .filter(s -> s.isSameChapter(2l)).findAny().get();
        //then
        assertThat(studyCheckAfterVerifyCheckRule.isDone()).isTrue();
    }

    @DisplayName("그룹 상태가 START가 아닌 상태(GATHERING, GATHER_COMPLETE, END)에서 그룹 체크 시 " +
            "NotAppropriateGroupStatusException 예외가 발생한다.")
    @ParameterizedTest
    @EnumSource(names = {"GATHERING","GATHER_COMPLETE", "END"})
    void verify_check_rule_fail_cause_not_appropriate_group_status(GroupStatus status) {
        //given - 그룹 시작 상태로 변경
        Group group = makeTestGroup(5);
        group.changeGroupStatusTo(status);
        group.initializeGroupTask(TestGroupFactory.studyCheckForms);

        //when - then
        assertThatThrownBy(() -> group.doTask(2l, 1l))
                .isInstanceOf(NotAppropriateGroupStatusException.class)
                .hasMessage(NOT_APPROPRIATE_GROUP_STATUS);
    }

    @DisplayName("그룹 체크 성공 케이스")
    @ParameterizedTest(name = "실패 원인 : {2} | 챕터 식별자 {0} | 그룹 멤버 식별자 {1}")
    @CsvSource(value = {"4, 1, 존재하지 않는 챕터", "1, 2, 존재하지 않는 그룹 멤버"})
    void verify_check_rule_fail_cause_not_exist_member_or_chapter(Long chapterId, Long groupMemberId) {
        //given - 그룹 시작 상태로 변경
        Group group = makeTestGroup(5);
        group.changeGroupStatusTo(START);
        group.initializeGroupTask(TestGroupFactory.studyCheckForms); // chapter 는 1,2,3 까지 존재합니다.

        //when - then
        //
        assertThatThrownBy(() -> group.doTask(chapterId, groupMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BAD_REQUEST);
    }

    @DisplayName("그룹 멤버가 updateGroupMemberLastVisitedTime를 호출할 경우 " +
            "해당 groupMember의 LastVisitedTime 이 업데이트 된다." +
            "그 결과 afterLastVisitedTime는 beforeLastVisitedTime보다 이후 시간을 가진다.")
    @Test
    void update_group_member_last_visited_time_case_myself() throws InterruptedException {
        //given
        Long groupMemberId = 1l;

        Group group = TestGroupFactory.receiveSampleGroup(groupMemberId);
        GroupMember groupMember = group.getGroupMembers().get(0);
        LocalDateTime beforeLastVisitedTime = groupMember.getLastVisitedTime();
        //when
        Thread.sleep(100);
        group.updateGroupMemberLastVisitedTime(groupMemberId);
        //then
        GroupMember updateGroupMember = group.getGroupMembers().get(0);
        LocalDateTime afterLastVisitedTime = updateGroupMember.getLastVisitedTime();

        assertThat(afterLastVisitedTime).isAfter(beforeLastVisitedTime);
    }

    @DisplayName("그룹 멤버가 아닌 사용자가 updateGroupMemberLastVisitedTime를 호출할 경우 " +
            "해당 groupMember의 LastVisitedTime은 업데이트 되지 않고 유지된다. " +
            "그 결과 afterLastVisitedTime 와 beforeLastVisitedTime는 동일하다.")
    @Test
    void update_group_member_last_visited_time_case_not_myself() throws InterruptedException {
        //given
        Long groupMemberId = 1l;
        Long notGroupMemberId = 100l;

        Group group = TestGroupFactory.receiveSampleGroup(groupMemberId);
        GroupMember groupMember = group.getGroupMembers().get(0);
        LocalDateTime beforeLastVisitedTime = groupMember.getLastVisitedTime();
        //when
        Thread.sleep(100);
        group.updateGroupMemberLastVisitedTime(notGroupMemberId);
        //then
        GroupMember updateGroupMember = group.getGroupMembers().get(0);
        LocalDateTime afterLastVisitedTime = updateGroupMember.getLastVisitedTime();

        assertThat(afterLastVisitedTime).isEqualTo(beforeLastVisitedTime);
    }

    @DisplayName("그룹 멤버가 스터디체크 조회 시, 그룹 멤버 자신의 스터디체크가 조회된다.")
    @Test
    void receive_study_checks_case_group_member() {
        //given
        Group group = TestGroupFactory.startedGroupSample(1l, 5);
        //when
        group.doTask(1l, 1l);
        List<Task> myTasks = group.receiveTasksOf(1l);

        List<Task> anotherMemberTasks = group.receiveTasksOf(2l);
        //then
        assertThat(myTasks).extracting("isDone").containsExactly(true, false, false);
        assertThat(anotherMemberTasks).extracting("isDone").containsExactly(false, false, false);
    }

    @DisplayName("그룹 생성 시, 기간(period)은 시작일이 종료일보다 앞서야 한다. " +
            "그렇지 않을 경우 IllegalArgumentException 예외" +
            "INCORRECT_PERIOD 메시지 발생")
    @Test
    void init_role_1_period() {
        //given
        Group group = Group.builder()
                .period(Period.of(LocalDate.of(2023, 12, 24), LocalDate.of(2022, 12, 24)))
                .host(new Host(1l, "xuni"))
                .capacity(Capacity.of(5))
                .build();

        Period period = group.getPeriod();

        assertThatCode(() -> period.verifyPeriod())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INCORRECT_PERIOD);
    }

    @DisplayName("그룹 생성 시, 시간(time)은 시작 시간이 종료 시간보다 앞서야 한다. " +
            "그렇지 않을 경우 IllegalArgumentException 예외" +
            "INCORRECT_TIME 메시지 발생")
    @Test
    void init_role_1_time() {
        //given
        Group group = Group.builder()
                .time(Time.of(LocalTime.of(23, 0, 0), LocalTime.of(22, 0, 0)))
                .host(new Host(1l, "xuni"))
                .capacity(Capacity.of(5))
                .build();

        Time time = group.getTime();

        assertThatCode(() -> time.verifyTime())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INCORRECT_TIME);
    }
}