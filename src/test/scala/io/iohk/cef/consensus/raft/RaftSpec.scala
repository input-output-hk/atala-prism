package io.iohk.cef.consensus.raft

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import io.iohk.cef.consensus.raft.MonitoringActor._
import io.iohk.cef.consensus.raft.protocol.{ClusterConfiguration, _}
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import io.iohk.cef.consensus.raft.protocol._

import scala.util.Random

abstract class RaftSpec(_system: Option[ActorSystem] = None)
    extends TestKit(_system getOrElse ActorSystem("raft-protocol-test"))
    with ImplicitSender
    with Eventually
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with PersistenceCleanup {

  val DefaultTimeout = 5

  import scala.concurrent.duration._
  val DefaultTimeoutDuration: FiniteDuration = DefaultTimeout.seconds

  override implicit val patienceConfig = PatienceConfig(
    timeout = scaled(Span(DefaultTimeout, Seconds)),
    interval = scaled(Span(200, Millis))
  )

  protected var members: Vector[ActorRef] = Vector.empty

  def initialMembers: Int

  lazy val config = system.settings.config
  lazy val electionTimeoutMin = config.getDuration("akka.raft.election-timeout.min", TimeUnit.MILLISECONDS).millis
  lazy val electionTimeoutMax = config.getDuration("akka.raft.election-timeout.max", TimeUnit.MILLISECONDS).millis

  implicit var probe: TestProbe = _

  var raftConfiguration: ClusterConfiguration = _

  var stateTransitionActor: ActorRef = _

  override def beforeAll() {
    super.beforeAll()
    stateTransitionActor = system.actorOf(Props(classOf[MonitoringActor]))
    (1 to initialMembers).toList foreach { i =>
      createActor(s"raft-member-$i")
    }
    raftConfiguration = ClusterConfiguration(members)
    members foreach { _ ! ChangeConfiguration(raftConfiguration) }

  }

  def createActor(name: String): ActorRef = {

    val actor = system.actorOf(Props(new SimpleCommandRaftActor).withDispatcher("akka.raft-dispatcher"), name)
    stateTransitionActor ! AddMember(actor)
    members :+= actor
    actor
  }

  override def beforeEach() {
    super.beforeEach()
    persistenceCleanup()
    probe = TestProbe()
  }

  override def afterEach() {
    system.eventStream.unsubscribe(probe.ref)
    super.afterEach()
  }

  def simpleName(ref: ActorRef): String = {
    import scala.collection.JavaConverters._
    ref.path.getElements.asScala.last
  }

  def leaders: Seq[Member] = {
    stateTransitionActor.tell(GetLeaders, probe.ref)
    val msg = probe.expectMsgClass(classOf[Leaders])
    msg.leaders
  }

  def candidates: Seq[Member] = {
    stateTransitionActor.tell(GetCandidates, probe.ref)
    val msg = probe.expectMsgClass(classOf[Candidates])
    msg.candidates
  }

  def followers: Seq[Member] = {
    stateTransitionActor.tell(GetFollowers, probe.ref)
    val msg = probe.expectMsgClass(classOf[Followers])
    msg.followers
  }

  def subscribeClusterStateTransitions(): Unit =
    stateTransitionActor ! Subscribe(members)

  def subscribeBeginAsLeader()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[BeginAsLeader])

  def awaitBeginAsLeader(max: FiniteDuration = DefaultTimeoutDuration)(implicit probe: TestProbe): BeginAsLeader =
    probe.expectMsgClass(max, classOf[BeginAsLeader])

  def subscribeForAppendEntries()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[AppendEntries[_]])


  def awaitExpectedHeartBeatAppendEntries(max: FiniteDuration = DefaultTimeoutDuration)(implicit probe: TestProbe): AppendEntries[_] =
    probe.expectMsgClass(max, classOf[AppendEntries[_]])


  def subscribeBeginElection()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[ElectionStarted])

  def subscribeElectionStarted()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[ElectionStarted])

  def subscribeTermUpdated()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[TermUpdated])

  def awaitBeginElection(max: FiniteDuration = DefaultTimeoutDuration)(implicit probe: TestProbe): Unit =
    probe.expectMsgClass(max, BeginElection.getClass)

  def awaitElectionStarted(max: FiniteDuration = DefaultTimeoutDuration)(implicit probe: TestProbe): ElectionStarted =
    probe.expectMsgClass(max, classOf[ElectionStarted])


  def subscribeBeginAsFollower()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[BeginAsFollower])


  def awaitBeginAsFollower(max: FiniteDuration = DefaultTimeoutDuration)
                          (implicit probe: TestProbe): BeginAsFollower =
    probe.expectMsgClass(max, classOf[BeginAsFollower])



  def subscribeEntryComitted()(implicit probe: TestProbe): Unit =
    system.eventStream.subscribe(probe.ref, classOf[EntryCommitted])

  def awaitEntryComitted(Index: Int, max: FiniteDuration = DefaultTimeoutDuration)(implicit probe: TestProbe): Unit = {
    val start = System.currentTimeMillis()
    probe.fishForMessage(max, hint = s"EntryCommitted($Index, actor)") {
      case EntryCommitted(Index, actor) =>
        info(s"Finished fishing for EntryCommitted($Index) on ${simpleName(actor)}, took ${System.currentTimeMillis() - start}ms")
        true

      case other =>
        info(s"Fished $other, still waiting for ${EntryCommitted(Index, null)}...")
        false
    }
  }

  def infoMemberStates() {
    val leadersList = leaders.map(m => s"${simpleName(m)}[Leader]")
    val candidatesList = candidates.map(m => s"${simpleName(m)}[Candidate]")
    val followersList = followers.map(m => s"${simpleName(m)}[Follower]")
    val members = (leadersList ++ candidatesList ++ followersList).mkString(",")
    info(s"Members: $members")
  }

  def killLeader() = {
    leaders.headOption.map { leader =>
      stateTransitionActor ! RemoveMember(leader)
      probe.watch(leader)
      system.stop(leader)
      probe.expectTerminated(leader)
      members = members.filterNot(_ == leader)
    }
  }


  def restartMember(_member: Option[ActorRef] = None) = {
    val member = _member.getOrElse(members(Random.nextInt(members.size)))
    stateTransitionActor ! RemoveMember(member)
    probe.watch(member)
    system.stop(member)
    members = members.filterNot(_ == member)
    probe.expectTerminated(member)
    val newMember = createActor(member.path.name)
    newMember
  }


  override def afterAll() {
    super.afterAll()
    shutdown(system)
  }
}
