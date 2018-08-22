package io.iohk.cef.consensus.raft

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import io.iohk.cef.consensus.raft.MonitoringActor._
import io.iohk.cef.consensus.raft.protocol.{ClusterConfiguration, _}
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}

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
    val actor = system.actorOf(Props(new SimpleCommandRaftActor), name)
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

  override def afterAll() {
    super.afterAll()
    shutdown(system)
  }
}
