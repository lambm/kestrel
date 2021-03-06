package net.lag.kestrel.loadtest.thrift

import com.twitter.finagle.kestrel.protocol.{Response => KestrelResponse}
import com.twitter.util.Future
import com.twitter.parrot.processor.ThriftRecordProcessor
import com.twitter.parrot.server.{ParrotRequest, ParrotService}
import com.twitter.parrot.thrift.ParrotJob
import net.lag.kestrel.loadtest.KestrelRecordProcessor
import net.lag.kestrel.thrift.Kestrel
import org.apache.thrift.protocol.TBinaryProtocol

abstract class AbstractKestrelThriftLoadTest[ResultType](parrotService: ParrotService[ParrotRequest, Array[Byte]])
extends ThriftRecordProcessor(parrotService) with KestrelRecordProcessor {
  val client = new Kestrel.FinagledClient(service, new TBinaryProtocol.Factory())

  def commands: Seq[(Kestrel.FinagledClient) => Future[ResultType]]

  def processLines(job: ParrotJob, lines: Seq[String]) {
    log.debug("process %d lines", lines.size)

    shuffle(commands).map { command =>
      try {
        command(client) onFailure { t => log.error(t, "request failed") }
      } catch {
        case ex => log.error(ex, "request generation error")
      }
    }
  }
}
