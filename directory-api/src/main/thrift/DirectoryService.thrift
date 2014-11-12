namespace java at.ac.tuwien.aic.ws14.group2.onion.directory.api.service

struct NodeUsage {
    1: string beginTime,
    2: string endTime,
    3: i64 relayMsgCount,
    4: i64 createMsgCount,
    5: optional string signature
}

struct NodeUsageSummary {
    1: string beginTime,
    2: string endTime,
    3: i64 relayMsgCount,
    4: i64 createMsgCount,
    5: optional string signature
}

struct ChainNodeInformation {
    1: i32 port,
    2: string address,
    3: optional string instanceName,
    4: string publicRsaKey //base64?
}

service DirectoryService {

   void ping(), //TODO remove

   bool heartbeat(1: ChainNodeInformation nodeInformation, 2: NodeUsage nodeUsage), //TODO switch to ID instead of nodeInformation?

   bool registerNode(1: ChainNodeInformation nodeInformation), //TODO return ID instead of bool?

   list<ChainNodeInformation> getChain(1: optional i32 chainLength = 3),

}