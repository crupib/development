include "cassandra.thrift"

namespace java org.apache.cassandra.thrift
namespace cpp org.apache.cassandra
namespace csharp Apache.Cassandra
namespace py cassandra
namespace php cassandra
namespace perl Cassandra
namespace rb CassandraThrift


/** Identifies what type of storage to use */
enum StorageType {
  CFS_REGULAR, CFS_ARCHIVE
}

struct LocalBlock
{
    1: required string file,
    2: required i64 offset,
    3: required i64 length,
    4: required bool compression
}

struct LocalOrRemoteBlock
{
    1: optional binary remote_block,
    2: optional LocalBlock local_block
}

struct dtidentifier
{
    1: required binary identifier,
    2: required binary password,
}

service Dse extends cassandra.Cassandra
{
  /**
   * Return the current SparkMaster address in this datacenter.
   */
  string describe_spark_master_address() throws (1:cassandra.InvalidRequestException ire),

  /**
   * Returns the current Job Tracker addres in this datacenter.
   */
  string describe_hadoop_job_tracker_address() throws (1:cassandra.InvalidRequestException ire),

  /**
   * Returns a set of Spark metrics configuration properties, which are retrieved from the
   * dse_system.spark_metrics_config table.
   */
  map<string, string> describe_spark_metrics_config() throws (1:cassandra.InvalidRequestException ire),

  /** Returns an IP address of the connected client as seen from the DSE server. */
  string describe_ip_mirror() throws (1:cassandra.InvalidRequestException ire),

  /**  returns (in order) the endpoints for each key specified. */
  list<list<string>> describe_keys(1:required string keyspace, 2:required list<binary> keys)
   throws (1:cassandra.InvalidRequestException ire, 2:cassandra.UnavailableException ue, 3:cassandra.TimedOutException te),

  /**
   * Returns a local or remote sub block.
   *
   * A remote sub block is the expected binary sub block data
   *
   * A local sub block is the file, offset and length for the calling application to read
   * This is a great optimization because it avoids any actual data transfer.
   *
   */
   LocalOrRemoteBlock get_cfs_sblock(
    1:required string caller_host_name,
    2:required binary block_id,
    3:required binary sblock_id,
    4:required i32 offset=0,
    6:string keyspace="cfs",
    7:cassandra.ConsistencyLevel consistency_level=ConsistencyLevel.QUORUM)
    throws (1:cassandra.InvalidRequestException ire, 2:cassandra.UnavailableException ue, 3:cassandra.TimedOutException te, 4:cassandra.NotFoundException nfe),

   /**
    * Returns a CFS sub block by copying it using thrift. Use it only if get_cfs_sblock fails e.g. due to permission problems.
    */
   LocalOrRemoteBlock get_remote_cfs_sblock(
    1:required binary block_id,
    2:required binary sblock_id,
    4:i32 offset=0,
    5:string keyspace = "cfs",
    6:cassandra.ConsistencyLevel consistency_level)
    throws (1:cassandra.InvalidRequestException ire, 2:cassandra.UnavailableException ue, 3:cassandra.TimedOutException te, 4:cassandra.NotFoundException nfe),

   /**
    * Generates a CassandraDelegateTokenIdentifier
    */
   dtidentifier get_delegation_token(1:string owner, 2:string renewer) throws (1:cassandra.InvalidRequestException ire)

   /**
    * Renews a CassandraDelegateTokenIdentifier
    */
   i64 renew_delegation_token(1:binary token_identifier) throws (1:cassandra.InvalidRequestException ire)
   
   /**
    * Removes a CassandraDelegateTokenIdentifier
    */
   void cancel_delegation_token(1:binary token_identifier) throws (1:cassandra.InvalidRequestException ire)
 
   /**
    * Check CFS for inconsistencies or missing blocks
    */
   string check_cfs(1:required string filename)
     
   /**
    * Returns true if the given user is a superuser of the CFS.
    */
   bool is_cfs_super_user(1:string user)

   /**
    * Returns DSE version string.
    */
   string get_dse_version()

   /**
    * Obtain the datacenter for the indicated node
    */
   string get_datacenter(1:string address) 
  
   /**
    * Obtain the live nodes
    */
   set<string> get_livenodes() 
}
